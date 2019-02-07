package com.wbq.raft.rpc;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
@Builder
@ToString
@Getter
public class RpcResponse<T> implements Serializable {

    private static final long serialVersionUID = 3822997022831107566L;

    private T data;
}
