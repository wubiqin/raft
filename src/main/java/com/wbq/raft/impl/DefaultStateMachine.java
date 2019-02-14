package com.wbq.raft.impl;

import com.wbq.raft.StateMachine;
import com.wbq.raft.pojo.LogEntry;

import java.util.List;

/**
 *  * @author biqin.wu
 *  * @since 14 February 2019
 *  
 */
public class DefaultStateMachine implements StateMachine {


    @Override
    public void apply(List<LogEntry> logEntries) {

    }

}
