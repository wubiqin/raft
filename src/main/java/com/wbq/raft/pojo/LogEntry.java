package com.wbq.raft.pojo;

import lombok.*;
import lombok.experimental.Wither;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@ToString
@EqualsAndHashCode
@Wither
@Data
@NoArgsConstructor
@AllArgsConstructor
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
