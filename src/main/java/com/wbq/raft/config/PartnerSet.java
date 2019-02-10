package com.wbq.raft.config;

import com.google.common.collect.Sets;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Wither;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  * @author biqin.wu  * @since 06 February 2019  
 */
@Builder
@Getter
@Wither
public class PartnerSet implements Serializable {

    private static final long serialVersionUID = 8890662240439938114L;

    private Set<Partner> partners = Sets.newHashSet();

    private volatile Partner leader;

    private volatile Partner self;

    private PartnerSet() {
    }

    private static class PartnerSetHolder {
        public static final PartnerSet HOLDER = new PartnerSet();
    }

    public boolean add(Partner partner) {
        return partners.add(partner);
    }

    public boolean remove(Partner partner) {
        return partners.remove(partner);
    }

    public Set<Partner> getOtherPartner() {
        return partners.stream().filter(it -> !it.equals(self)).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "PartnerSet{" + "partners=" + partners + ", leader=" + leader + ", self=" + self + '}';
    }
}
