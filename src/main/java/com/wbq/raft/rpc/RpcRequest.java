package com.wbq.raft.rpc;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
@Builder
@Getter
@ToString
public class RpcRequest<T> implements Serializable {

    private static final long serialVersionUID = -3322295305231662636L;

    private Integer type;

    private T data;

    private String url;
}
