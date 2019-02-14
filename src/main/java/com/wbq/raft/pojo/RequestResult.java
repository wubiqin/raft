package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Wither;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@Getter
@Wither
public class RequestResult implements Serializable {

    private static final long serialVersionUID = 4068521629159257258L;

    public static final RequestResult fail = RequestResult.builder().success(false).build();
    /**
     * 当前的任期号
     */
    private Long term;
    /**
     * preIndex 和 preTerm匹配
     */
    private Boolean success;
}
