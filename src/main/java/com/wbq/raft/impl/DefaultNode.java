package com.wbq.raft.impl;

import com.wbq.raft.Node;
import com.wbq.raft.change.ClusterListener;
import com.wbq.raft.change.Result;
import com.wbq.raft.config.NodeConfig;
import com.wbq.raft.config.NodeStatus;
import com.wbq.raft.config.Partner;
import com.wbq.raft.config.PartnerSet;
import com.wbq.raft.pojo.ClientRequest;
import com.wbq.raft.pojo.ClientResponse;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  * @author biqin.wu  * @since 10 February 2019  
 */
@Slf4j
public class DefaultNode implements Node, ClusterListener {
    /**
     * 选举时间间隔
     */
    private volatile long electionTime = 10 * 1000;
    /**
     * 上一次选举时间
     */
    private volatile long preElectionTime = 0;
    /**
     * 上一次心跳时间
     */
    private volatile long preHeartBeatTime = 0;
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
    private volatile long currentTerm = 0;
    /**
     * 当前获得选票的server
     * <p>
     * ip:port
     * </p>
     */
    private volatile String votedServerId;

    private RpcClient rpcClient = new DefaultRpcClient();

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
     * 心跳任务 leader节点才需要
     */
    class HeartBeatTask implements Runnable {

        @Override
        public void run() {
            if (status == NodeStatus.LEADER) {
                return;
            }
            long cur = System.currentTimeMillis();
            if (cur - preHeartBeatTime < heartBeatTick) {
                return;
            }

            log.info("*********NextIndex************");
            partnerSet.getOtherPartner()
                    .forEach(peer -> log.info("peer={},nextIndex={}", peer.getAddress(), nextIndex.get(peer)));

            preHeartBeatTime = System.currentTimeMillis();

            partnerSet.getOtherPartner().forEach(this::sendHeartBeat);

        }

        private void sendHeartBeat(Partner peer) {
            RequestParam param = RequestParam.builder().leaderId(partnerSet.getLeader().getAddress())
                    .logEntries(Collections.emptyList()).serverId(peer.getAddress()).term(currentTerm).build();

            RpcRequest request = RpcRequest.builder().type(RpcRequest.Type.APPEND_ENTRIES).data(param)
                    .url(peer.getAddress()).build();

            RaftThreadPool.execute(() -> {
                try {
                    RpcResponse response = rpcClient.send(request);
                    RequestResult result = (RequestResult) response.getData();
                    long term = result.getTerm();

                    if (term > currentTerm) {
                        log.info("node will become follower node-address={},node-term={} leader-term={}",
                                peer.getAddress(), currentTerm, term);
                        currentTerm = term;
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
}
