package com.wbq.raft.pojo;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
public class BaseParam implements Serializable {

    private static final long serialVersionUID = 8657738683757952848L;
    /**
     * 任期号
     */
    private long term;
    /**
     * 请求者 ip:port
     */
    private String serverId;
}
