package com.wbq.raft.pojo;

import com.wbq.raft.config.Partner;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
@Builder
@Getter
@ToString
public class Replication<T> implements Serializable {

    private static final long serialVersionUID = -1574158518151670952L;

    public static final String COUNT_SUFFIX = "_count";

    public static final String SUCCESS_SUFFIX = "_success";

    private String countKey;

    private String successKey;

    private Callable<T> callable;

    private LogEntry logEntry;

    private Partner partner;

    private Long offTime;

}
