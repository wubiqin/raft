package com.wbq.raft.rpc;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
public interface RpcClient {

    RpcResponse send(RpcRequest request);
}
