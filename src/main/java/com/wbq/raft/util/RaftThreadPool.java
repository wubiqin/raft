package com.wbq.raft.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
public final class RaftThreadPool {

    private static final int corePoolSize = 20;

    private static final int maxPoolSize = 20;

    private static final int queueSize = 10000;

    private static final int keepAliveTime = 10;

    private static final BlockingQueue<Runnable> queue = new LinkedBlockingDeque<>(queueSize);

    private static final RaftThreadPoolExecutor executor = new RaftThreadPoolExecutor(corePoolSize, maxPoolSize,
            keepAliveTime, TimeUnit.SECONDS, queue);

    private static final ScheduledExecutorService se = new ScheduledThreadPoolExecutor(corePoolSize,
            new RaftThreadFactory("raft-schedule"));

    private RaftThreadPool() {
    }

    public static void scheduleAtFixedRate(Runnable r, long initialDelay, long period) {
        se.scheduleAtFixedRate(r, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public static void scheduleAtFixDelay(Runnable r, long period) {
        se.scheduleWithFixedDelay(r, 0, period, TimeUnit.MILLISECONDS);
    }

    public static <T> Future<T> submit(Callable<T> c) {
        return executor.submit(c);
    }

    public static void execute(Runnable r) {
        executor.execute(r);
    }

}
