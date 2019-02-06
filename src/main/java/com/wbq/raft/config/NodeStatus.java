package com.wbq.raft.config;

/**
 *  * @author biqin.wu
 *  * @since 06 February 2019
 *  
 */
public class NodeStatus {

    public static final int FOLLOWER = 0;

    public static final int CANDIDATE = 1;

    public static final int LEADER = 2;

    private enum Status {
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

        Status(int code) {
            this.code = code;
        }

        public static Status codeOf(int code) {
            for (Status status : Status.values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException(String.format("not such enum of code :%s", code));
        }
    }
}
