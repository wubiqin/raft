package com.wbq.raft.change;

import com.wbq.raft.config.Partner;

/**
 * <p>
 * 集群成员监听
 * </p>
 *  * @author biqin.wu  * @since 07 February 2019  
 */
public interface ClusterListener {
    /**
     * 添加节点
     * 
     * @param partner
     * @return
     */
    Result addPeer(Partner partner);

    /**
     * 移除节点
     * 
     * @param partner
     * @return
     */
    Result removePeer(Partner partner);
}
