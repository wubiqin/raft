package com.wbq.raft.rpc;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
@Builder
@Getter
@ToString
public class RpcRequest<T> implements Serializable {

    private static final long serialVersionUID = -3322295305231662636L;

    private Type type;

    private T data;

    private String url;

    public enum Type {
        /**
         * 
         */
        VOTE(0, "请求投票"),
        /**
         * 
         */
        APPEND_ENTRIES(1, "附加日志"),
        /**
         * 
         */
        CLIENT(2, "客户端"),
        /**
         * 
         */
        CONFIG_ADD(3, "添加节点"),
        /**
         * 
         */
        CONFIG_REMOVE(4, "移除节点");

        private int code;

        private String msg;

        Type(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
