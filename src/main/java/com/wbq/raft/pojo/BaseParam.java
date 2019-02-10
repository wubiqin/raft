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
public class BaseParam implements Serializable {

    private static final long serialVersionUID = 8657738683757952848L;
    /**
     * 任期号
     */
    private Long term;
    /**
     * 请求者 ip:port
     */
    private String serverId;
}
