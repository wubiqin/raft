package com.wbq.raft.common;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
public enum RequestType {
    /**
     * 
     */
    GET(0),
    /**
     * 
     */
    PUT(1);

    private final int code;

    RequestType(int code) {
        this.code = code;
    }

    public static RequestType codeOf(int code) {
        for (RequestType type : RequestType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("not such request type =%s", code));
    }
}
