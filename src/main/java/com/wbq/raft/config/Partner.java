package com.wbq.raft.config;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

/**
 *  * @author biqin.wu
 *  * @since 06 February 2019
 *  
 */
@Builder
@Getter
public class Partner implements Serializable {

    private static final long serialVersionUID = -6489922923574393815L;
    /**
     * ip:port
     */
    private final String address;

    public Partner(String address) {
        this.address = address;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Partner)) {
            return false;
        }

        Partner p = (Partner) obj;

        return Objects.equals(p.address, address);
    }


    @Override
    public String toString() {
        return "Partner{" +
                "address='" + address + '\'' +
                '}';
    }
}
