package com.wbq.raft.rpc;

import com.alipay.remoting.BizContext;
import com.wbq.raft.Node;
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
        return null;
    }
}
