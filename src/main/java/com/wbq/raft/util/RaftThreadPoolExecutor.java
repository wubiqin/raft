package com.wbq.raft.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  * @author biqin.wu
 *  * @since 06 February 2019
 *  
 */
public class RaftThreadPoolExecutor extends ThreadPoolExecutor {

    private static final Logger log = LoggerFactory.getLogger(RaftThreadPoolExecutor.class);

    private static final ThreadLocal<Long> COST_TIME = ThreadLocal.withInitial(System::currentTimeMillis);

    public RaftThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new RaftThreadFactory("raft"), new RejectHandler(new ThreadPoolExecutor.AbortPolicy()));
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        COST_TIME.get();
        log.debug("raft thread start");
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        log.debug("raft thread cost={}", System.currentTimeMillis() - COST_TIME.get());
        COST_TIME.remove();
    }

    @Override
    protected void terminated() {
        super.terminated();
        log.info("active thread count={},queueSize={},poolSize={}", getActiveCount(), getQueue().size(), getPoolSize());
    }

    private static class RejectHandler implements RejectedExecutionHandler {

        private RejectedExecutionHandler rejectedExecutionHandler;

        RejectHandler(RejectedExecutionHandler rejectedExecutionHandler) {
            this.rejectedExecutionHandler = rejectedExecutionHandler;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            rejectedExecutionHandler.rejectedExecution(r, executor);
        }
    }
}