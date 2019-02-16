package com.wbq.raft.impl;

import com.google.common.base.Strings;
import com.wbq.raft.Consensus;
import com.wbq.raft.config.NodeStatus;
import com.wbq.raft.config.Partner;
import com.wbq.raft.pojo.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * 默认一致性实现
 * </p>
 *  * @author biqin.wu  * @since 10 February 2019  
 */
@Getter
@Slf4j
public class DefaultConsensus implements Consensus {

    private DefaultNode node;

    private final ReentrantLock voteLock = new ReentrantLock();

    private final ReentrantLock appendLock = new ReentrantLock();

    public DefaultConsensus(DefaultNode node) {
        this.node = node;
    }

    @Override
    public VoteResult vote(VoteParam param) {
        try {
            if (!voteLock.tryLock()) {
                return VoteResult.fail;
            }
            // 对方节点的任期没有当前节点新
            if (param.getTerm() < node.getCurrentTerm().get()) {
                return VoteResult.builder().success(false).term(node.getCurrentTerm().get()).build();
            }
            // 节点没有投票或者节点已经投票给当前节点 并且候选人的日志term>=节点 节点就投票
            if (Strings.isNullOrEmpty(node.getVotedServerId()) || node.getVotedServerId().equals(param.getServerId())) {
                log.info("node={} vote for ={}  param serverId={}", node.getPartnerSet().getSelf(),
                        node.getVotedServerId(), param.getServerId());
                log.info("node={} current term={},param term={}", node.getPartnerSet().getSelf(), node.getCurrentTerm(),
                        param.getTerm());
                if (node.getLogModule().lastLog() != null) {
                    if (node.getLogModule().lastLog().getTerm() > param.getLastLogTerm()) {
                        return VoteResult.fail;
                    }

                    if (node.getLogModule().lastLogIndex() > param.getLastLogIndex()) {
                        return VoteResult.fail;
                    }
                }

                node.setStatus(NodeStatus.FOLLOWER);
                node.getPartnerSet().setLeader(Partner.builder().address(param.getServerId()).build());
                node.setVotedServerId(param.getServerId());
                return VoteResult.builder().term(node.getCurrentTerm().get()).success(true).build();
            }

            return VoteResult.builder().term(node.getCurrentTerm().get()).success(false).build();
        } finally {
            voteLock.unlock();
        }
    }

    @Override
    public RequestResult appendLog(RequestParam param) {
        try {
            if (!appendLock.tryLock()) {
                return RequestResult.fail;
            }
            if (param.getTerm() < node.getCurrentTerm().get()) {
                return RequestResult.fail.withTerm(node.getCurrentTerm().get());
            }

            node.setPreHeartBeatTime(new AtomicLong(System.currentTimeMillis()));
            node.setPreElectionTime(new AtomicLong(System.currentTimeMillis()));
            node.getPartnerSet().setLeader(Partner.builder().address(param.getLeaderId()).build());

            if (param.getTerm() >= node.getCurrentTerm().get()) {
                log.info("node={} become follower currentTerm={},param term={},serverId={}",
                        node.getPartnerSet().getSelf(), node.getCurrentTerm(), param.getTerm(), param.getServerId());
                node.setStatus(NodeStatus.FOLLOWER);
            }

            node.setCurrentTerm(new AtomicLong(param.getTerm()));

            // 心跳
            if (CollectionUtils.isEmpty(param.getLogEntries())) {
                log.info("node={} append heartbeat success leader term={},node term={}", node.getPartnerSet().getSelf(),
                        param.getTerm(), node.getCurrentTerm().get());
                return RequestResult.builder().success(true).term(node.getCurrentTerm().get()).build();
            }

            if (node.getLogModule().lastLogIndex() != 0 && param.getPreIndex() != 0) {
                LogEntry logEntry;
                if ((logEntry = node.getLogModule().read(param.getPreIndex())) != null) {
                    if (!logEntry.getTerm().equals(param.getPreTerm())) {
                        // 日志在索引处的term不匹配
                        // 需要减小nextIndex重试 todo 确认
                        return RequestResult.fail.withTerm(node.getCurrentTerm().get());
                    }
                } else {
                    return RequestResult.fail.withTerm(node.getCurrentTerm().get());
                }
            }

            // 如果已经存在的日志和新的冲突，索引相同但是term不相同，删除这一条之后的日志
            LogEntry logEntry = node.getLogModule().read(param.getPreIndex() + 1);
            if (logEntry != null && !logEntry.getTerm().equals(param.getLogEntries().get(0).getTerm())) {
                node.getLogModule().removeFrom(param.getPreIndex() + 1);
            } else if (logEntry != null) {
                return RequestResult.builder().success(true).term(node.getCurrentTerm().get()).build();
            }

            // 写入日志 应用到状态机
            for (LogEntry l : param.getLogEntries()) {
                node.getLogModule().write(l);
                node.getStateMachine().apply(Collections.singletonList(l));
            }

            if (param.getLeaderCommitIndex() > node.getCommitIndex()) {
                long index = Math.min(param.getLeaderCommitIndex(), node.getCommitIndex());
                node.setCommitIndex(index);
                node.setLastApplied(index);
            }

            node.setStatus(NodeStatus.FOLLOWER);
            return RequestResult.builder().success(true).term(node.getCurrentTerm().get()).build();
        } finally {
            appendLock.unlock();
        }
    }
}
