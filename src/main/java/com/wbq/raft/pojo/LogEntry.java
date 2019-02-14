package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Wither;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@ToString
@Getter
@EqualsAndHashCode
@Wither
public class LogEntry implements Serializable, Comparable {

    private static final long serialVersionUID = 3078982288100998255L;

    private Long index;

    private Long term;

    private Command command;

    @Override
    public int compareTo(Object o) {
        assert o != null;
        if (!(o instanceof LogEntry)) {
            throw new IllegalArgumentException();
        }
        LogEntry logEntry = (LogEntry) o;
        return Long.compare(this.getIndex(), logEntry.getIndex());
    }
}
