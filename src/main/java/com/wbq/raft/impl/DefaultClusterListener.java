package com.wbq.raft.impl;

import com.wbq.raft.change.ClusterListener;
import com.wbq.raft.change.Result;
import com.wbq.raft.config.NodeStatus;
import com.wbq.raft.config.Partner;
import com.wbq.raft.pojo.LogEntry;
import com.wbq.raft.rpc.RpcRequest;
import com.wbq.raft.rpc.RpcResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: biqin.wu
 * @Date: 2019/2/14
 * @Time: 14:30
 * @Description: 集群配置变更监听默认实现
 */
@Slf4j
public class DefaultClusterListener implements ClusterListener {

    private final DefaultNode node;

    public DefaultClusterListener(DefaultNode node) {
        this.node = node;
    }

    @Override
    public Result addPeer(Partner partner) {
        synchronized (node) {
            if (node.getPartnerSet().getPartners().contains(partner)) {
                log.info("该节点已经存在 node={}", partner.toString());
                return Result.instance;
            }
            node.getPartnerSet().getPartners().add(partner);

            if (node.getStatus() == NodeStatus.LEADER) {
                node.getNextIndex().put(partner, 0L);
                node.getMatchIndex().put(partner, 0L);
                // 同步日志
                for (long i = 0; i < node.getLogModule().lastLogIndex(); i++) {
                    LogEntry logEntry = node.getLogModule().read(i);
                    if (logEntry != null) {
                        node.replication(partner, logEntry);
                    }
                }

                // 同步到其他节点上
                for (Partner p : node.getPartnerSet().getOtherPartner()) {
                    RpcRequest request = RpcRequest.builder().type(RpcRequest.Type.CONFIG_ADD).url(partner.getAddress())
                            .data(partner).build();

                    RpcResponse response = node.getRpcClient().send(request);
                    Result result = (Result) response.getData();

                    if (result != null && result.getStatus() == Result.SUCCESS) {
                        log.info("replication success node={} newNode={}", p, partner);
                    } else {
                        log.warn("replication fail node={},newNode={}", p, partner);
                    }
                }

            }
        }

        return Result.instance;
    }

    @Override
    public Result removePeer(Partner partner) {
        synchronized (node) {
            node.getPartnerSet().getPartners().remove(partner);
            node.getNextIndex().remove(partner);
            node.getMatchIndex().remove(partner);
        }
        return Result.instance;
    }
}
