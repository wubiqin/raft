package com.wbq.raft.config;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Set;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@Getter
@ToString
public class NodeConfig implements Serializable {

    private static final long serialVersionUID = -4559044960402813096L;

    private Integer port;
    /**
     * all node address
     */
    private Set<String> addresses;
}
