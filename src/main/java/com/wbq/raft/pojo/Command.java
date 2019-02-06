package com.wbq.raft.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
@Getter
@Builder
@ToString
public class Command implements Serializable {

    private static final long serialVersionUID = -8225400619096569881L;

    private String key;

    private String val;
}
