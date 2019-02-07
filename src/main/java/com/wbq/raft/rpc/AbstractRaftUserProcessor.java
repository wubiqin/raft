package com.wbq.raft.rpc;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;

/**
 *  * @author biqin.wu  * @since 08 February 2019  
 */
public abstract class AbstractRaftUserProcessor<T> extends AbstractUserProcessor<T> {

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) {
        throw new UnsupportedOperationException(
                "raft rpc server not supported  handleRequest(BizContext bizCtx, AsyncContext asyncCtx, Object request)");
    }

    @Override
    public String interest() {
        return RpcRequest.class.getName();
    }
}
