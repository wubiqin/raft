package com.wbq.raft.pojo;

import lombok.*;

import java.io.Serializable;

/**
 *  * @author biqin.wu  * @since 07 February 2019  
 */
@Getter
@Builder
@ToString
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Command implements Serializable {

    private static final long serialVersionUID = -8225400619096569881L;

    private String key;

    private String val;
}
