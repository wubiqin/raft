package com.wbq.raft.impl;

import com.wbq.raft.Log;
import com.wbq.raft.Node;
import com.wbq.raft.StateMachine;
import com.wbq.raft.change.ClusterListener;
import com.wbq.raft.change.Result;
import com.wbq.raft.config.NodeConfig;
import com.wbq.raft.config.NodeStatus;
import com.wbq.raft.config.Partner;
import com.wbq.raft.config.PartnerSet;
import com.wbq.raft.pojo.ClientRequest;
import com.wbq.raft.pojo.ClientResponse;
import com.wbq.raft.pojo.LogEntry;
import com.wbq.raft.pojo.Replication;
import com.wbq.raft.pojo.RequestParam;
import com.wbq.raft.pojo.RequestResult;
import com.wbq.raft.pojo.VoteParam;
import com.wbq.raft.pojo.VoteResult;
import com.wbq.raft.rpc.DefaultRpcClient;
import com.wbq.raft.rpc.RpcClient;
import com.wbq.raft.rpc.RpcRequest;
import com.wbq.raft.rpc.RpcResponse;
import com.wbq.raft.util.RaftThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 *  * @author biqin.wu  * @since 10 February 2019  
 */
@Slf4j
public class DefaultNode implements Node, ClusterListener {
    /**
     * 选举时间间隔 todo 目前更新为单线程 理论上可以使用volatile
     */
    private AtomicLong electionTime = new AtomicLong(10 * 1000);
    /**
     * 上一次选举时间
     */
    private AtomicLong preElectionTime = new AtomicLong();
    /**
     * 上一次心跳时间
     */
    private AtomicLong preHeartBeatTime = new AtomicLong();
    /**
     * 心跳间隔
     */
    private final long heartBeatTick = 5 * 1000;

    private volatile NodeStatus status;

    private PartnerSet partnerSet;

    private Map<Partner, Long> nextIndex = new ConcurrentHashMap<>();
    /**
     * 对于每一个服务器 已经复制给他的日志的最高索引值
     */
    private Map<Partner, Long> matchIndex = new ConcurrentHashMap<>();
    /**
     * 服务器所知的最新的任期
     */
    private AtomicLong currentTerm = new AtomicLong();
    /**
     * 当前获得选票的server
     * <p>
     * ip:port
     * </p>
     */
    private volatile String votedServerId;

    private RpcClient rpcClient = new DefaultRpcClient();
    /**
     * 日志条目集
     */
    private Log logModule;
    /**
     * 心跳任务
     */
    private HeartBeatTask heartBeatTask = new HeartBeatTask();
    /**
     * 选举任务
     */
    private ElectionTask electionTask = new ElectionTask();
    /**
     * 失败的复制队列
     */
    private LinkedBlockingQueue<Replication> replicationFailQueue = new LinkedBlockingQueue<>(2048);
    /**
     * 状态机
     */
    private StateMachine stateMachine;

    @Override
    public void setConfig(NodeConfig config) {

    }

    @Override
    public VoteResult handleVote(VoteParam param) {
        return null;
    }

    @Override
    public RequestResult handleAppendLog(RequestParam param) {
        return null;
    }

    @Override
    public ClientResponse handleClientRequest(ClientRequest request) {
        return null;
    }

    @Override
    public ClientResponse redirect(ClientRequest request) {
        return null;
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {

    }

    @Override
    public Result addPeer(Partner partner) {
        return null;
    }

    @Override
    public Result removePeer(Partner partner) {
        return null;
    }

    /**
     * 心跳任务 节点为leader时才需要
     */
    class HeartBeatTask implements Runnable {

        @Override
        public void run() {
            if (status == NodeStatus.LEADER) {
                return;
            }
            long cur = System.currentTimeMillis();
            if (cur - preHeartBeatTime.get() < heartBeatTick) {
                return;
            }

            log.info("*********NextIndex************");
            partnerSet.getOtherPartner()
                    .forEach(peer -> log.info("peer={},nextIndex={}", peer.getAddress(), nextIndex.get(peer)));

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
            electionTime.getAndAdd(ThreadLocalRandom.current().nextLong(50));
            if (cur - preElectionTime.get() < electionTime.get()) {
                return;
            }
            status = NodeStatus.CANDIDATE;
            log.info("node={} will become candidate and start election ,current term ={},lastEntry={}",
                    partnerSet.getSelf(), currentTerm, logModule.lastLog());
            preElectionTime.getAndAdd(System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(200) + 150);
            currentTerm.addAndGet(1);

            votedServerId = partnerSet.getSelf().getAddress();
            Set<Partner> peers = partnerSet.getOtherPartner();
            List<Future<RpcResponse>> futureList = getRpcFutures(peers);

            log.info("futureList count={}", futureList.size());
            AtomicInteger votedCount = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(futureList.size());
            log.info("latch count={}", latch.getCount());

            getVoteResult(futureList, votedCount, latch);

            try {
                latch.await(3500, TimeUnit.MICROSECONDS);
            } catch (InterruptedException e) {
                log.debug("InterruptedException by master election task");
            }

            log.info("node={} may become leader,get voted count={},node status={}", partnerSet.getSelf(),
                    votedCount.get(), status);
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
                RaftThreadPool.execute(() -> {
                    try {
                        @SuppressWarnings("unchecked")
                        RpcResponse<VoteResult> response = (RpcResponse<VoteResult>) future.get(3000, TimeUnit.SECONDS);
                        if (response == null) {
                            return;
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
                    } catch (Exception e) {
                        log.error("future.get() error e", e);
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
            })).filter(Objects::nonNull).collect(Collectors.toList());
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
                        // 清空队列中的消息 todo check
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
                        // 应用到状态机

                    }

                } catch (InterruptedException e) {
                    log.error("ignore InterruptedException");
                    e.printStackTrace();
                } catch (ExecutionException | TimeoutException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }
}
