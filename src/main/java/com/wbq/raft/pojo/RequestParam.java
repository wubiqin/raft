package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@ToString
@Getter
public class RequestParam<T> extends BaseParam {

    private String leaderId;

    private Long preIndex;

    private Long preTerm;
    /**
     * 准备存储的日志 为空时代表心跳
     */
    private List<LogEntry<T>> logEntries;
    /**
     * leader 已经提交的日志的index
     */
    private Long leaderCommitIndex;

}
