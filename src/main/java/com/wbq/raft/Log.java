package com.wbq.raft;

import com.wbq.raft.pojo.LogEntry;

import java.util.List;

/**
 * <p>
 * 日志实现
 * </p>
 *   @author biqin.wu  * @since 07 February 2019  
 */
public interface Log {
    void write(List<LogEntry> logEntries);

    LogEntry read(int index);

    void removeFrom(int index);

    LogEntry lastLog();

    long lastLogIndex();
}
