package com.wbq.raft.config;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
public enum NodeStatus {

    /**
     * default
     */
    FOLLOWER(0),
    /**
     * join vote
     */
    CANDIDATE(1),
    /**
     * leader
     */
    LEADER(2);

    private int code;

    NodeStatus(int code) {
        this.code = code;
    }

    public static NodeStatus codeOf(int code) {
        for (NodeStatus status : NodeStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("not such enum of code :%s", code));
    }

}
