package com.wbq.raft.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ThreadFactory;

/**
 *  * @author biqin.wu
 *  * @since 06 February 2019
 *  
 */
public class RaftThreadFactory implements ThreadFactory {

    private final ThreadFactoryBuilder builder;

    private final String prefix;

    private ThreadFactory threadFactory;

    public RaftThreadFactory(String prefix, boolean daemon) {
        this.builder = new ThreadFactoryBuilder();
        this.builder.setDaemon(daemon);
        this.builder.setNameFormat(prefix + "-thread-%d");
        this.prefix = prefix;
    }

    public RaftThreadFactory(String prefix) {
        this(prefix, true);
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        if (threadFactory == null) {
            synchronized (this) {
                if (threadFactory == null) {
                    threadFactory = this.builder.build();
                }
            }
        }
        return this.threadFactory.newThread(r);
    }
}
