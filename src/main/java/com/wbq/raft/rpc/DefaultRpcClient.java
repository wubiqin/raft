package com.wbq.raft.rpc;

import com.alipay.remoting.exception.RemotingException;
import lombok.extern.slf4j.Slf4j;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
@Slf4j
public class DefaultRpcClient implements RpcClient {

    private static final com.alipay.remoting.rpc.RpcClient RPC_CLIENT = new com.alipay.remoting.rpc.RpcClient();

    static {
        RPC_CLIENT.init();
    }

    @Override
    public RpcResponse send(RpcRequest request) {
        try {
            return (RpcResponse) RPC_CLIENT.invokeSync(request.getUrl(), request, 5000);
        } catch (RemotingException | InterruptedException e) {
            log.error("fail to send with rpc", e);
            throw new RuntimeException(e);
        }
    }
}
