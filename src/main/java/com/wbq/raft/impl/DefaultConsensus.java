package com.wbq.raft.impl;

import com.wbq.raft.Consensus;
import com.wbq.raft.Node;
import com.wbq.raft.pojo.RequestParam;
import com.wbq.raft.pojo.RequestResult;
import com.wbq.raft.pojo.VoteParam;
import com.wbq.raft.pojo.VoteResult;
import lombok.Getter;

import java.util.concurrent.locks.ReentrantLock;

/**
 *  * @author biqin.wu  * @since 10 February 2019  
 */
@Getter
public class DefaultConsensus implements Consensus {

    private Node node;

    private final ReentrantLock voteLock = new ReentrantLock();

    private final ReentrantLock appendLock = new ReentrantLock();

    public DefaultConsensus(Node node) {
        this.node = node;
    }

    @Override
    public VoteResult vote(VoteParam param) {
        if (!voteLock.tryLock()) {
            return VoteResult.builder().build();
        }
        return null;
    }

    @Override
    public RequestResult appendLog(RequestParam param) {
        return null;
    }
}
