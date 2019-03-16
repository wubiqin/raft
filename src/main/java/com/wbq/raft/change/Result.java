package com.wbq.raft.change;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 10 February 2019  a
 */
@Getter
@Builder
@ToString
public class Result implements Serializable {

    public static final int SUCCESS = 1;

    public static final int FAIL = 0;

    public static Result instance = Result.builder().build();

    private static final long serialVersionUID = 2164640805318868914L;

    private Integer status;
}
