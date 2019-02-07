package com.wbq.raft.rpc;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
public interface RpcServer {

    void start();

    void stop();

    RpcResponse handle(RpcRequest request);
}
