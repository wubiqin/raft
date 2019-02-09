package com.wbq.raft;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
public interface LifeCycle {
    /**
     * 初始化
     * 
     * @throws Throwable throw exception
     */
    void init() throws Throwable;

    /**
     * 关闭
     * 
     * @throws Throwable throw exception
     */
    void destroy() throws Throwable;
}
