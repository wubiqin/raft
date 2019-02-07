package com.wbq.raft;

import com.wbq.raft.pojo.RequestParam;
import com.wbq.raft.pojo.RequestResult;
import com.wbq.raft.pojo.VoteParam;
import com.wbq.raft.pojo.VoteResult;

/**
 * <p>
 * 一致性实现
 * </p>
 *  * @author biqin.wu  * @since 06 February 2019  
 */
public interface Consensus {
    /**
     * 请求投票
     * <p>
     * 接受者：</br>
     * 如果term < currentTerm return <t>false</t></br>
     * 如果节点没有投票或者节点已经投票给当前节点 并且候选人的日志term>=节点 节点就投票
     * </p>
     */
    VoteResult vote(VoteParam param);

    /**
     * 附加日志
     * <p>
     * 接受者实现：</br>
     * 如果term < currentTerm return <t>false</t> </br>
     * 如果preIndex和preTerm不匹配 return<t>false</t> </br>
     * 如果已存在的日志和新的冲突 index相同但是term不同 删除index和之后的 然后写入状态机</br>
     * </p>
     */
    RequestResult appendLog(RequestParam param);
}
