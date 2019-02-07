package com.wbq.raft;

import com.wbq.raft.pojo.LogEntry;

import java.util.List;

/**
 * <p>
 * 状态机实现
 * </p>
 *  * @author biqin.wu  * @since 07 February 2019  
 */
public interface StateMachine {
    /**
     * 将数据应用到状态机
     * 
     * @param logEntries 日志
     */
    void apply(List<LogEntry> logEntries);
}
