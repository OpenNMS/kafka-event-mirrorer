/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.tools.kem.mirror;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.core.ipc.sink.api.Message;
import org.opennms.netmgt.trapd.TrapDTO;
import org.opennms.netmgt.trapd.TrapIdentityDTO;
import org.opennms.netmgt.trapd.TrapLogDTO;
import org.opennms.tools.kem.config.TrapCriteria;
import org.opennms.tools.kem.config.Traps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.OID;

import com.codahale.metrics.MetricRegistry;

public class TrapSinkModuleMirrorer extends XmlSinkModuleMirrorer<TrapLogDTO> {
    private static final Logger LOG = LoggerFactory.getLogger(TrapSinkModuleMirrorer.class);

    private final Traps config;
    private final List<TrapMatcher> matchers;

    public TrapSinkModuleMirrorer(MetricRegistry metrics, Traps config) {
        super(metrics, TrapLogDTO.class, "traps");
        this.config = Objects.requireNonNull(config);
        matchers = toMatchers(config.getTrapCriteria());
    }

    @Override
    public TrapLogDTO mapIfNeedsForwarding(Message message) {
        final TrapLogDTO trapLogDTO = (TrapLogDTO)message;

        // Filter the traps
        final List<TrapDTO> trapsToForward = trapLogDTO.getMessages().stream()
                .filter(t -> {
                    final TrapIdentityDTO trapIdentity = t.getTrapIdentity();
                    final Optional<TrapMatcher> matcher = matchers.stream().filter(m -> m.matches(trapIdentity)).findFirst();
                    if (!matcher.isPresent()) {
                        return false;
                    }
                    logMatch(matcher.get().getStatKey());
                    return true;
                })
                .collect(Collectors.toList());

        // Track the number of traps we did not forward
        messagesFiltered.mark(trapLogDTO.getMessages().size() - trapsToForward.size());

        // Nothing to forward?
        if (trapsToForward.size() < 1) {
            return null;
        }

        // Optionally override the system id
        String effectiveSystemId = trapLogDTO.getSystemId();
        if (config.getSystemIdOverride() != null) {
            effectiveSystemId = config.getSystemIdOverride();
        }

        // Optionally override the location
        String effectiveLocation = trapLogDTO.getLocation();
        if (config.getLocationOverride() != null) {
            effectiveLocation = config.getLocationOverride();
        }

        // Rebuild the log
        final TrapLogDTO filteredTrapLogDTO = new TrapLogDTO();
        filteredTrapLogDTO.setLocation(effectiveLocation);
        filteredTrapLogDTO.setSystemId(effectiveSystemId);
        filteredTrapLogDTO.setTrapAddress(trapLogDTO.getTrapAddress());
        filteredTrapLogDTO.setMessages(trapsToForward);

        if (LOG.isDebugEnabled()) {
            final String traps = filteredTrapLogDTO.getMessages().stream()
                    .map(m -> String.format("Trap[enterprise: %s, generic: %s, specific: %s]",
                            m.getTrapIdentity().getEnterpriseId(), m.getTrapIdentity().getGeneric(), m.getTrapIdentity().getSpecific()))
                    .collect(Collectors.joining(","));
            LOG.debug("Forwarding trap log from {}: {}", filteredTrapLogDTO.getTrapAddress(), traps);
        }

        return filteredTrapLogDTO;
    }

    private static class TrapMatcher {
        private final TrapCriteria criteria;
        private final OID enterpriseOid;
        private final boolean matchNone;
        private String statKey;

        public TrapMatcher(TrapCriteria criteria) {
            this.criteria = Objects.requireNonNull(criteria);
            enterpriseOid = criteria.getEnterprise() != null ? new OID(criteria.getEnterprise()) : null;
            // If everything is null, match none
            matchNone = enterpriseOid == null && criteria.getGeneric() == null && criteria.getSpecific() == null;
            // Compute the stat key
            statKey = String.format("%s/%d/%d", criteria.getEnterprise(), criteria.getGeneric(), criteria.getSpecific());
        }

        public boolean matches(TrapIdentityDTO trapIdentity) {
            if (matchNone) {
                return false;
            }
            if (enterpriseOid != null && !Objects.equals(enterpriseOid, new OID(trapIdentity.getEnterpriseId()))) {
                return false;
            }
            if (criteria.getGeneric() != null && criteria.getGeneric() != trapIdentity.getGeneric()) {
                return false;
            }
            return criteria.getSpecific() == null || criteria.getSpecific() == trapIdentity.getSpecific();
        }

        public String getStatKey() {
            return statKey;
        }
    }

    private static List<TrapMatcher> toMatchers(List<TrapCriteria> trapCriteria) {
        return trapCriteria.stream()
                .map(TrapMatcher::new)
                .collect(Collectors.toList());
    }

    @Override
    public long getNumMessagesIn(Message message) {
        final TrapLogDTO trapLogDTO = (TrapLogDTO)message;
        return trapLogDTO.getMessages().size();
    }

    @Override
    public String getSourceTopic() {
        return config.getSourceTopic();
    }

    @Override
    public String getTargetTopic() {
        return config.getTargetTopic();
    }

}
