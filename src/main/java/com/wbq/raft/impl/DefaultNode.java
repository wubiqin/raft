package com.wbq.raft.impl;

import com.wbq.raft.Log;
import com.wbq.raft.Node;
import com.wbq.raft.change.ClusterListener;
import com.wbq.raft.change.Result;
import com.wbq.raft.config.NodeConfig;
import com.wbq.raft.config.NodeStatus;
import com.wbq.raft.config.Partner;
import com.wbq.raft.config.PartnerSet;
import com.wbq.raft.pojo.ClientRequest;
import com.wbq.raft.pojo.ClientResponse;
import com.wbq.raft.pojo.LogEntry;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 *  * @author biqin.wu  * @since 10 February 2019  
 */
@Slf4j
public class DefaultNode implements Node, ClusterListener {
    /**
     * 选举时间间隔
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
            List<Future> futureList = peers.stream().map(peer -> RaftThreadPool.submit(() -> {
                long lastTerm = 0L;
                LogEntry logEntry = logModule.lastLog();
                if (logEntry != null) {
                    lastTerm = logEntry.getTerm();
                }

                VoteParam param = VoteParam.builder().serverId(partnerSet.getSelf().getAddress()).lastLogTerm(lastTerm)
                        .term(currentTerm.get()).lastLogIndex((long) logModule.lastLogIndex()).build();
                RpcRequest request = RpcRequest.builder().type(RpcRequest.Type.VOTE).url(peer.getAddress()).data(param)
                        .build();
                try {
                    return rpcClient.send(request);
                } catch (Exception e) {
                    log.error("ElectionTask fail url={}", request.getUrl());
                    return null;
                }
            })).filter(Objects::nonNull).collect(Collectors.toList());

            log.info("voteResult futureList={}", futureList.size());
            AtomicBoolean success = new AtomicBoolean();
            CountDownLatch latch = new CountDownLatch(futureList.size());


        }
    }
}
