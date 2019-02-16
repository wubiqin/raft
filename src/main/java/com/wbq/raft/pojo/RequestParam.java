package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Wither;

import java.io.Serializable;
import java.util.List;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@ToString
@Getter
@Wither
public class RequestParam implements Serializable {

    private static final long serialVersionUID = 3142476754276804342L;
    /**
     * 任期号
     */
    private Long term;
    /**
     * 请求者 ip:port
     */
    private String serverId;

    private String leaderId;

    private Long preIndex;

    private Long preTerm;
    /**
     * 准备存储的日志 为空时代表心跳
     */
    private List<LogEntry> logEntries;
    /**
     * leader 已经提交的日志的index
     */
    private Long leaderCommitIndex;

}
