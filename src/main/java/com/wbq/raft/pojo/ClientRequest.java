package com.wbq.raft.pojo;

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
public class ClientRequest implements Serializable {

    private static final long serialVersionUID = -7810929167526096098L;

    private Integer type;

    private String key;

    private String val;
}
