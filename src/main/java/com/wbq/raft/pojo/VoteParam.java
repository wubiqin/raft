package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@Getter
@ToString
public class VoteParam implements Serializable {

    private static final long serialVersionUID = -4489764005911710102L;
    /**
     * 请求选票的候选人
     */
    private String serverId;

    private Long term;

    private Long lastLogIndex;

    private Long lastLogTerm;
}
