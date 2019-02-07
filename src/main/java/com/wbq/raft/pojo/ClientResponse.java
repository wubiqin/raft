package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
@Builder
@Getter
public class ClientResponse<T> implements Serializable {

    private static final long serialVersionUID = -7718781000912683050L;

    private String msg;

    private T data;

}
