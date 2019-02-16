package com.wbq.raft.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.wbq.raft.Consensus;
import com.wbq.raft.Log;
import com.wbq.raft.Node;
import com.wbq.raft.StateMachine;
import com.wbq.raft.change.ClusterListener;
import com.wbq.raft.change.Result;
import com.wbq.raft.common.RequestType;
import com.wbq.raft.config.NodeConfig;
import com.wbq.raft.config.NodeStatus;
import com.wbq.raft.config.Partner;
import com.wbq.raft.config.PartnerSet;
import com.wbq.raft.pojo.*;
import com.wbq.raft.rpc.*;
import com.wbq.raft.util.RaftThreadPool;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 *  * @author biqin.wu  * @since 10 February 2019  
 */
@Slf4j
@Getter
public class DefaultNode implements Node, ClusterListener {
    /**
     * 选举时间间隔
     */
    private AtomicLong electionTime = new AtomicLong(10 * 1000);
    /**
     * 上一次选举时间
     */
    @Setter
    private AtomicLong preElectionTime = new AtomicLong();
    /**
     * 上一次心跳时间
     */
    @Setter
    private AtomicLong preHeartBeatTime = new AtomicLong();
    /**
     * 心跳间隔
     */
    private final long heartBeatTick = 5 * 1000;
    /**
     * 节点当前的状态 #{@link NodeStatus}
     */
    @Setter
    private volatile NodeStatus status = NodeStatus.FOLLOWER;

    private PartnerSet partnerSet;

    private Map<Partner, Long> nextIndex = new ConcurrentHashMap<>();
    /**
     * 对于每一个服务器 已经复制给他的日志的最高索引值
     */
    private Map<Partner, Long> matchIndex = new ConcurrentHashMap<>();
    /**
     * 服务器所知的最新的任期
     */
    @Setter
    private AtomicLong currentTerm = new AtomicLong();
    /**
     * 当前获得选票的server
     * <p>
     * ip:port
     * </p>
     */
    @Setter
    private volatile String votedServerId;

    /* ============ 所有服务器上经常变的 ============= */
    /**
     * 已知的最大的被提交的日志条目的索引
     */
    @Setter
    private volatile long commitIndex;
    /**
     * 最后被应用到状态机的日志条目索引值
     */
    @Setter
    private volatile long lastApplied;

    private RpcClient rpcClient = new DefaultRpcClient();

    private RpcServer rpcServer;
    /**
     * 日志条目集
     */
    private Log logModule = DefaultLog.getInstance();
    /**
     * 心跳任务
     */
    private HeartBeatTask heartBeatTask = new HeartBeatTask();
    /**
     * 选举任务
     */
    private ElectionTask electionTask = new ElectionTask();
    /**
     * 消费失败的任务
     */
    private ReplicationFailQueueConsumer replicationFailQueueConsumer = new ReplicationFailQueueConsumer();
    /**
     * 失败的复制队列
     */
    private LinkedBlockingQueue<Replication> replicationFailQueue = new LinkedBlockingQueue<>(2048);
    /**
     * 状态机
     */
    private StateMachine stateMachine = DefaultStateMachine.getInstance();
    /**
     * 一致性
     */
    private Consensus consensus;
    /**
     * 集群成员监听
     */
    private ClusterListener clusterListener;

    private volatile boolean started;

    private NodeConfig config;

    private DefaultNode() {
    }

    public static DefaultNode getInstance() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final DefaultNode INSTANCE = new DefaultNode();
    }

    @Override
    public void init() throws Throwable {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            rpcServer.start();

            consensus = new DefaultConsensus(this);
            clusterListener = new DefaultClusterListener(this);

            RaftThreadPool.scheduleAtFixDelay(heartBeatTask, 500);
            RaftThreadPool.scheduleAtFixedRate(electionTask, 6000, 500);
            RaftThreadPool.execute(replicationFailQueueConsumer);

            LogEntry logEntry = logModule.lastLog();
            if (logEntry != null) {
                currentTerm.set(logEntry.getTerm());
            }

            started = true;
            log.info("node stated success node={}", partnerSet.getSelf());
        }
    }

    @Override
    public void setConfig(NodeConfig config) {
        this.config = config;
        partnerSet = PartnerSet.builder().partners(Sets.newHashSet()).build();

        for (String addr : config.getAddresses()) {
            Partner partner = Partner.builder().address(addr).build();
            partnerSet.add(partner);
            String local = String.format("localhost:%s", config.getPort());
            if (local.equals(addr)) {
                partnerSet = partnerSet.withSelf(partner);
            }
        }

        rpcServer = new DefaultRpcServer(config.getPort(), this);
    }

    @Override
    public VoteResult handleVote(VoteParam param) {
        log.warn("invoke handleVote param={}", param);
        return consensus.vote(param);
    }

    @Override
    public RequestResult handleAppendLog(RequestParam param) {
        log.warn("invoke handleAppendLog param={}", param);
        return consensus.appendLog(param);
    }

    /**
     * 这里需要保证同步
     * 
     * @param request
     * @return
     */
    @Override
    public synchronized ClientResponse handleClientRequest(ClientRequest request) {
        log.info("client invoke type={},key={},val={}", RequestType.codeOf(request.getType()).name(), request.getKey(),
                request.getVal());
        if (status != NodeStatus.LEADER) {
            log.warn("node={} is not leader  redirect to leader={}", partnerSet.getSelf().getAddress(),
                    partnerSet.getLeader().getAddress());
            return redirect(request);
        }

        if (RequestType.codeOf(request.getType()) == RequestType.GET) {
            LogEntry logEntry = stateMachine.get(request.getKey());
            if (logEntry != null) {
                return ClientResponse.builder().data(logEntry.getCommand()).build();
            }
            return ClientResponse.builder().data(null).build();
        }

        LogEntry logEntry = LogEntry.builder()
                .command(Command.builder().key(request.getKey()).val(request.getVal()).build()).term(currentTerm.get())
                .build();

        logModule.write(logEntry);
        log.warn("write logEntry finish ligEntry={}", logEntry);

        final AtomicLong success = new AtomicLong(0);

        // 复制到其他节点上
        List<Future<Boolean>> futures = partnerSet.getOtherPartner().stream().map(peer -> replication(peer, logEntry))
                .collect(Collectors.toList());
        futures.forEach(future -> RaftThreadPool.execute(() -> {
            try {
                if (future.get(3000, TimeUnit.MILLISECONDS)) {
                    success.incrementAndGet();
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("replication fail");
            }
        }));

        List<Long> matchIndexList = matchIndex.values().stream().sorted().collect(Collectors.toList());

        if (matchIndexList.size() < 2) {
            log.info("only one node in cluster node={}", partnerSet.getSelf());
            return apply(logEntry);
        }
        // 如果存在 i>commitIndex 代表大多数matchIndex>=commitIndex
        // 而且log在i处的term==currentTerm 则令commitIndex=i
        int medium = matchIndexList.size() / 2;
        long i = matchIndexList.get(medium);

        if (i > commitIndex) {
            LogEntry l = logModule.read(i);
            if (l != null && l.getTerm() == currentTerm.get()) {
                commitIndex = i;
            }
        }
        // 超过一半成功
        if (success.get() > futures.size() / 2) {
            return apply(logEntry);
        } else {
            logModule.removeFrom(logEntry.getIndex());
            log.warn("fail apply to local state machine logEntry={}", logEntry);
            return ClientResponse.builder().msg("fail").build();
        }
    }

    private ClientResponse apply(LogEntry logEntry) {
        commitIndex = logEntry.getIndex();
        stateMachine.apply(Collections.singletonList(logEntry));
        lastApplied = commitIndex;

        log.info("success apply to local stateMachine logEntry info={}", logEntry);
        return ClientResponse.builder().msg("success").build();
    }

    @Override
    public ClientResponse redirect(ClientRequest request) {
        log.info("request redirect url={}", partnerSet.getLeader().getAddress());
        RpcRequest req = RpcRequest.builder().url(partnerSet.getLeader().getAddress()).type(RpcRequest.Type.CLIENT)
                .data(request).build();
        RpcResponse response = rpcClient.send(req);
        return (ClientResponse) response.getData();
    }

    @Override
    public void destroy() throws Throwable {
        rpcServer.stop();
    }

    @Override
    public Result addPeer(Partner partner) {
        return clusterListener.addPeer(partner);
    }

    @Override
    public Result removePeer(Partner partner) {
        return clusterListener.removePeer(partner);
    }

    public Future<Boolean> replication(Partner partner, LogEntry logEntry) {
        return RaftThreadPool.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                {
                    long start = System.currentTimeMillis(), end = start;

                    while (end - start <= 20 * 1000) {
                        RequestParam param = RequestParam.builder().leaderCommitIndex(commitIndex)
                                .term(currentTerm.get()).leaderId(partnerSet.getLeader().getAddress())
                                .logEntries(Collections.singletonList(logEntry)).build();
                        // todo check
                        long next = getNextIndex().get(partner);
                        LinkedList<LogEntry> logEntries = new LinkedList<>();
                        if (logEntry.getIndex() >= next) {
                            for (long i = next; i < logEntry.getIndex(); i++) {
                                LogEntry l = logModule.read(i);
                                if (l != null) {
                                    logEntries.add(l);
                                }
                            }
                        } else {
                            logEntries.add(logEntry);
                        }

                        LogEntry preLog = getPreLog(logEntries.getFirst());
                        param = param.withPreIndex(preLog.getIndex()).withPreTerm(preLog.getTerm());

                        RpcRequest request = RpcRequest.builder().type(RpcRequest.Type.APPEND_ENTRIES)
                                .url(partner.getAddress()).data(param).build();

                        try {
                            RpcResponse response = rpcClient.send(request);
                            if (response == null) {
                                return false;
                            }
                            RequestResult result = (RequestResult) response.getData();
                            if (result != null && result.getSuccess()) {
                                log.info("append log success follower={} entry={}", partner, logEntry);

                                nextIndex.put(partner, logEntry.getIndex() + 1);
                                matchIndex.put(partner, logEntry.getIndex() + 1);
                                return true;
                            } else if (result != null) {
                                if (result.getTerm() > currentTerm.get()) {
                                    log.info("follower term ={}, my term={} node become follower={}", result.getTerm(),
                                            currentTerm.get(), partnerSet.getSelf());

                                    currentTerm.set(result.getTerm());
                                    status = NodeStatus.FOLLOWER;
                                    return false;
                                }
                            } else {
                                if (next == 0L) {
                                    next = 1L;
                                }
                                nextIndex.put(partner, next - 1);
                                log.warn("follower={} nextIndex not match reduce nextIndex and retry  nextIndex={} ",
                                        partner, next);
                            }
                            end = System.currentTimeMillis();
                        } catch (Exception e) {
                            log.error(e.getMessage());
                            // todo 放队列重试
                            Replication replication = Replication.builder().callable(this).logEntry(logEntry)
                                    .partner(partner).offTime(System.currentTimeMillis()).build();
                            replicationFailQueue.add(replication);
                            return false;
                        }

                    }
                    return false;
                }
            }
        });

    }

    private LogEntry getPreLog(LogEntry logEntry) {
        LogEntry entry = logModule.read(logEntry.getIndex() - 1);

        if (entry == null) {
            log.warn("get perLog is null , parameter logEntry : {}", logEntry);
            entry = LogEntry.builder().index(0L).term(0L).command(null).build();
        }
        return entry;
    }

    /**
     * 心跳任务 节点为leader时才需要
     */
    class HeartBeatTask implements Runnable {

        @Override
        public void run() {
            if (status != NodeStatus.LEADER) {
                return;
            }
            long cur = System.currentTimeMillis();
            if (cur - preHeartBeatTime.get() < heartBeatTick) {
                return;
            }

            log.info("*********NextIndex************");
            partnerSet.getOtherPartner().forEach(peer -> {
                log.info("leader={}", partnerSet.getLeader().getAddress());
                log.info("peer={},nextIndex={}", peer.getAddress(), nextIndex.get(peer));
            });

            preHeartBeatTime.set(System.currentTimeMillis());

            partnerSet.getOtherPartner().forEach(this::sendHeartBeat);

        }

        private void sendHeartBeat(Partner peer) {
            RequestParam param = RequestParam.builder().leaderId(partnerSet.getLeader().getAddress())
                    .logEntries(Collections.emptyList()).serverId(peer.getAddress()).term(currentTerm.get()).build();

            RpcRequest request = RpcRequest.builder().type(RpcRequest.Type.APPEND_ENTRIES).data(param)
                    .url(peer.getAddress()).build();

            RaftThreadPool.execute(() -> {
                try {
                    RpcResponse response = rpcClient.send(request);
                    RequestResult result = (RequestResult) response.getData();
                    long term = result.getTerm();

                    if (term > currentTerm.get()) {
                        log.info("node will become follower node-address={},node-term={} leader-term={}",
                                peer.getAddress(), currentTerm, term);
                        currentTerm.set(term);
                        votedServerId = Strings.EMPTY;
                        status = NodeStatus.FOLLOWER;
                    }
                } catch (Exception e) {
                    log.error("fail to send HeartBeatTask with rpc request url={},error={}", request.getUrl(),
                            e.getMessage());
                }
            });
        }
    }

    /**
     * 选举任务：在转变为候选者后开始选举过程
     * <p>
     * 1：自增当前的任期号</br>
     * 2：投票给自己</br>
     * 3：重置选举超时计时器</br>
     * 4：发送请求投票给其他服务器</br>
     * </p>
     * <p>
     * 1:如果收到超过一半人的投票 就变成leader</br>
     * 2:如果收到日志就变成follower</br>
     * 3:如果选举过程超时就重新发起一轮选举
     * </p>
     * 
     */
    class ElectionTask implements Runnable {

        @Override
        public void run() {
            if (status == NodeStatus.LEADER) {
                return;
            }
            long cur = System.currentTimeMillis();
            // raft随机时间 解决冲突
            electionTime.addAndGet(ThreadLocalRandom.current().nextLong(50));
            if (cur - preElectionTime.get() < electionTime.get()) {
                return;
            }
            status = NodeStatus.CANDIDATE;
            log.info("node={} will become candidate and start election ,current term ={},lastEntry={}",
                    partnerSet.getSelf(), currentTerm, logModule.lastLog());
            preElectionTime.set(System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(200) + 150);
            currentTerm.addAndGet(1);

            votedServerId = partnerSet.getSelf().getAddress();
            Set<Partner> peers = partnerSet.getOtherPartner();
            List<Future<RpcResponse>> futureList = getRpcFutures(peers);

            AtomicInteger votedCount = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(futureList.size());
            log.info("latch count={}", latch.getCount());

            getVoteResult(futureList, votedCount, latch);

            try {
                latch.await(3500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.debug("InterruptedException by master election task");
            }

            log.info("node={} may become leader,get voted count={},node status={}", partnerSet.getSelf(),
                    votedCount.get(), status.name());
            // 如果投票期间，有其他服务器发送 appendEntry , 就可能变成 follower ,这时,应该停止
            if (status == NodeStatus.FOLLOWER) {
                return;
            }
            // 成为leader
            if (votedCount.get() >= partnerSet.getPartners().size() / 2) {
                becomeLeader();
            } else {
                // 重新选举
                votedServerId = Strings.EMPTY;
            }

        }

        private void becomeLeader() {
            log.info("node ={}become leader", partnerSet.getSelf());
            status = NodeStatus.LEADER;
            partnerSet = partnerSet.withLeader(partnerSet.getSelf());
            votedServerId = Strings.EMPTY;
            // 初试化所有节点的nextIndex为自己最后一条日志的index+1,如果下次rpc时 follow和leader不一致，失败
            // leader将会递减index重试
            nextIndex = new ConcurrentHashMap<>(16);
            matchIndex = new ConcurrentHashMap<>(16);
            for (Partner partner : partnerSet.getOtherPartner()) {
                nextIndex.put(partner, logModule.lastLogIndex() + 1);
                matchIndex.put(partner, 0L);
            }
        }

        private void getVoteResult(List<Future<RpcResponse>> futureList, AtomicInteger votedCount,
                CountDownLatch latch) {
            for (Future<RpcResponse> future : futureList) {
                RaftThreadPool.submit(() -> {
                    try {
                        @SuppressWarnings("unchecked")
                        RpcResponse<VoteResult> response = (RpcResponse<VoteResult>) future.get(3000,
                                TimeUnit.MILLISECONDS);
                        if (response == null) {
                            return -1;
                        }
                        boolean voteGranted = response.getData().getSuccess();
                        if (voteGranted) {
                            votedCount.getAndIncrement();
                        } else {
                            long resultTerm = response.getData().getTerm();
                            if (resultTerm >= currentTerm.get()) {
                                // 更新任期
                                currentTerm.set(resultTerm);
                            }
                        }
                        return 0;
                    } catch (Exception e) {
                        log.error("future.get() error e", e);
                        return -1;
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        private List<Future<RpcResponse>> getRpcFutures(Set<Partner> peers) {
            return peers.stream().map(peer -> RaftThreadPool.submit(() -> {
                long lastTerm = 0L;
                LogEntry logEntry = logModule.lastLog();
                if (logEntry != null) {
                    lastTerm = logEntry.getTerm();
                }

                VoteParam param = VoteParam.builder().serverId(partnerSet.getSelf().getAddress()).lastLogTerm(lastTerm)
                        .term(currentTerm.get()).lastLogIndex(logModule.lastLogIndex()).build();
                RpcRequest request = RpcRequest.builder().type(RpcRequest.Type.VOTE).url(peer.getAddress()).data(param)
                        .build();
                try {
                    return rpcClient.send(request);
                } catch (Exception e) {
                    log.error("ElectionTask fail url={}", request.getUrl());
                    return null;
                }
            })).collect(Collectors.toList());
        }
    }

    /**
     * 消费失败的复制 todo 使用消息队列？</br>
     * 只有leader需要
     */
    class ReplicationFailQueueConsumer implements Runnable {
        /**
         * 一分钟
         */
        private final long interval = 60 * 1000;

        @Override
        public void run() {
            // 死循环 todo 修改为监听者模式
            for (;;) {
                try {
                    // 这里拿不到的话会阻塞
                    Replication r = replicationFailQueue.take();
                    if (status == NodeStatus.LEADER) {
                        log.info("当前节点已经不是leader了 leader={} 清空队列中的数据 node={},queue size={}",
                                partnerSet.getLeader().getAddress(), partnerSet.getSelf().getAddress(),
                                replicationFailQueue.size());
                        // 直接清空队列中的消息 todo check
                        replicationFailQueue.clear();
                    }
                    log.warn("ReplicationFailQueueConsumer take a task will retry replication,content detail={}",
                            r.getLogEntry());
                    if (System.currentTimeMillis() - r.getOffTime() > interval) {
                        log.warn("replication fail queue may full or handle slow");
                    }
                    Callable callable = r.getCallable();
                    @SuppressWarnings("unchecked")
                    Future<Boolean> future = RaftThreadPool.submit(callable);
                    // 重试成功
                    if (future.get(3000, TimeUnit.MILLISECONDS)) {
                        // 有机会应用到状态机
                        String successCount = stateMachine.getString(r.getSuccessKey());
                        stateMachine.set(r.getSuccessKey(), String.valueOf(Integer.valueOf(successCount) + 1));

                        String count = stateMachine.getString(r.getCountKey());
                        if (Integer.valueOf(successCount) + 1 >= Integer.valueOf(count) / 2) {
                            stateMachine.apply(Collections.singletonList(r.getLogEntry()));
                            stateMachine.del(Lists.newArrayList(r.getCountKey(), r.getSuccessKey()));
                        }
                    }

                } catch (InterruptedException e) {
                    log.error("ignore InterruptedException");
                } catch (ExecutionException | TimeoutException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }
}
