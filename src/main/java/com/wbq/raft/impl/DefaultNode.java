package com.wbq.raft.impl;

import com.wbq.raft.Node;
import com.wbq.raft.change.ClusterListener;
import com.wbq.raft.change.Result;
import com.wbq.raft.config.NodeConfig;
import com.wbq.raft.config.Partner;
import com.wbq.raft.pojo.ClientRequest;
import com.wbq.raft.pojo.ClientResponse;
import com.wbq.raft.pojo.RequestParam;
import com.wbq.raft.pojo.RequestResult;
import com.wbq.raft.pojo.VoteParam;
import com.wbq.raft.pojo.VoteResult;
import lombok.extern.slf4j.Slf4j;

/**
 *  * @author biqin.wu  * @since 10 February 2019  
 */
@Slf4j
public class DefaultNode implements Node, ClusterListener {
    @Override
    public void setConfig(NodeConfig config) {

    }

    @Override
    public VoteResult handleVote(VoteParam param) {
        return null;
    }

    @Override
    public RequestResult handleAppendLog(RequestParam param) {
        return null;
    }

    @Override
    public ClientResponse handleClientRequest(ClientRequest request) {
        return null;
    }

    @Override
    public ClientResponse redirect(ClientRequest request) {
        return null;
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destory() throws Throwable {

    }

    @Override
    public Result addPeer(Partner partner) {
        return null;
    }

    @Override
    public Result removePeer(Partner partner) {
        return null;
    }
}
