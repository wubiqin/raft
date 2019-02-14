package com.wbq.raft;

import com.wbq.raft.pojo.LogEntry;

/**
 * <p>
 * 日志实现
 * </p>
 *   @author biqin.wu  * @since 07 February 2019  
 */
public interface Log {
    void write(LogEntry logEntry);

    /**
     * may return null
     * @param index
     * @return
     */
    LogEntry read(long index);

    void removeFrom(long index);

    LogEntry lastLog();

    long lastLogIndex();
}
