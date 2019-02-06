package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@Getter
public class VoteResult implements Serializable {

    private static final long serialVersionUID = -6589292223655994378L;

    private Long term;

    private Boolean success;
}
