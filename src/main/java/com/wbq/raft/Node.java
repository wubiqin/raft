package com.wbq.raft;

import com.wbq.raft.config.NodeConfig;
import com.wbq.raft.pojo.ClientRequest;
import com.wbq.raft.pojo.ClientResponse;
import com.wbq.raft.pojo.RequestParam;
import com.wbq.raft.pojo.RequestResult;
import com.wbq.raft.pojo.VoteParam;
import com.wbq.raft.pojo.VoteResult;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
public interface Node extends LifeCycle {
    /**
     * 设置配置文件
     * 
     * @param config 配置
     */
    void setConfig(NodeConfig config);

    /**
     * 处理投票
     * 
     * @param param
     * @return
     */
    VoteResult handleVote(VoteParam param);

    /**
     * 处理附加日志
     * 
     * @param param 日志为空时代表心跳
     * @return
     */
    RequestResult handleAppendLog(RequestParam param);

    /**
     * 处理客户端请求
     * 
     * @param request
     * @return
     */
    ClientResponse handleClientRequest(ClientRequest request);

    /**
     * 转发给leader
     * 
     * @param request
     * @return
     */
    ClientResponse redirect(ClientRequest request);
}
