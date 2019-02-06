package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@Getter
public class RequestResult implements Serializable {

    private static final long serialVersionUID = 4068521629159257258L;
    /**
     * 当前的任期号
     */
    private long term;
    /**
     * preIndex 和 preTerm匹配
     */
    private boolean success;
}
