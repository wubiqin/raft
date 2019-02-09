package com.wbq.raft.rpc;

import com.alipay.remoting.BizContext;
import com.wbq.raft.Node;
import com.wbq.raft.change.ClusterListener;
import com.wbq.raft.config.Partner;
import com.wbq.raft.pojo.ClientRequest;
import com.wbq.raft.pojo.RequestParam;
import com.wbq.raft.pojo.VoteParam;
import lombok.extern.slf4j.Slf4j;

/**
 *  * @author biqin.wu  * @since 08 February 2019  
 */
@Slf4j
public class DefaultRpcServer implements RpcServer {
    private Node node;

    private com.alipay.remoting.rpc.RpcServer server;

    public DefaultRpcServer(int port, Node node) {

        server = new com.alipay.remoting.rpc.RpcServer(port, false, false);
        server.registerUserProcessor(new AbstractRaftUserProcessor<RpcRequest>() {
            @Override
            public Object handleRequest(BizContext bizCtx, RpcRequest request) throws Exception {
                return handle(request);
            }
        });

        this.node = node;
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop();
    }

    @Override
    public RpcResponse handle(RpcRequest request) {
        switch (request.getType()) {
        case VOTE:
            return RpcResponse.builder().data(node.handleVote((VoteParam) request.getData())).build();
        case APPEND_ENTRIES:
            return RpcResponse.builder().data(node.handleAppendLog((RequestParam) request.getData())).build();
        case CLIENT:
            return RpcResponse.builder().data(node.handleClientRequest((ClientRequest) request.getData())).build();
        case CONFIG_ADD:
            return RpcResponse.builder().data(((ClusterListener) node).addPeer((Partner) request.getData())).build();
        case CONFIG_REMOVE:
            return RpcResponse.builder().data(((ClusterListener) node).removePeer((Partner) request.getData())).build();
        default:
            break;
        }
        throw new RuntimeException();
    }
}
