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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.opennms.core.ipc.sink.api.Message;
import org.opennms.netmgt.syslogd.api.SyslogMessageDTO;
import org.opennms.netmgt.syslogd.api.SyslogMessageLogDTO;
import org.opennms.tools.kem.config.Syslog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;

public class SyslogSinkModuleMirrorer extends XmlSinkModuleMirrorer<SyslogMessageLogDTO> {
    private static final Logger LOG = LoggerFactory.getLogger(TrapSinkModuleMirrorer.class);
    private final static Pattern MAX_MIN_THRESHOLD_XML_PATTERN = Pattern.compile("<max.*Min>", Pattern.DOTALL | Pattern.MULTILINE);
    private final static Pattern SOURCE_ADDRESS_PATTERN = Pattern.compile(".*(\\s*source_address=(.*))\\s*$", Pattern.DOTALL | Pattern.MULTILINE);

    private final Syslog config;

    final List<Pattern> excludePatterns;
    final List<Pattern> includePatterns;
    final List<String> excludeContaining;
    final List<String> includeContaining;

    public SyslogSinkModuleMirrorer(MetricRegistry metrics, Syslog config) {
        super(metrics, SyslogMessageLogDTO.class, "syslog");
        this.config = Objects.requireNonNull(config);

        excludePatterns = config.getExcludeMessagesMatching().stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
        includePatterns = config.getIncludeMessagesMatching().stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
        excludeContaining = config.getExcludeMessagesContaining();
        includeContaining = config.getIncludeMessagesContaining();
    }

    @Override
    public SyslogMessageLogDTO mapIfNeedsForwarding(Message message) {
        final SyslogMessageLogDTO syslogMessageLogDTO = (SyslogMessageLogDTO)message;

        // Filter the messages
        final List<SyslogMessageDTO> messagesToForward = syslogMessageLogDTO.getMessages().stream()
                .filter(m -> {
                    final String syslog = new String(m.getBytes().array(), StandardCharsets.UTF_8);
                    // Excludes
                    for (String excludeContainingEntry : excludeContaining) {
                        if (syslog.contains(excludeContainingEntry)) {
                            return false;
                        }
                    }
                    for (Pattern excludePattern : excludePatterns) {
                        if (excludePattern.matcher(syslog).matches()) {
                            return false;
                        }
                    }

                    // Includes
                    for (String includeContainingEntry : includeContaining) {
                        if (syslog.contains(includeContainingEntry)) {
                            return true;
                        }
                    }
                    for (Pattern includePattern : includePatterns) {
                        if (includePattern.matcher(syslog).matches()) {
                            return true;
                        }
                    }

                    return false;
                })
                .collect(Collectors.toList());

        // Track the number of syslogs we did not forward
        messagesFiltered.mark(syslogMessageLogDTO.getMessages().size() - messagesToForward.size());

        // Nothing to forward?
        if (messagesToForward.size() < 1) {
            return null;
        }

        // Unmangle the syslog messages
        InetAddress sourceAddressOverride = null;
        for (SyslogMessageDTO msg : messagesToForward) {
            String content = new String(msg.getBytes().array(), StandardCharsets.UTF_8);
            final Matcher m = SOURCE_ADDRESS_PATTERN.matcher(content);
            if (m.matches()) {
                try {
                    sourceAddressOverride = InetAddress.getByName(m.group(2));
                } catch (UnknownHostException e) {
                    LOG.warn("Failed to get source address from: {}", m.group(2));
                }
                // Truncate the message and update the DTO
                content = content.substring(0, m.start(1));
                msg.setBytes(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
            }
        }

        String effectiveLocation = syslogMessageLogDTO.getLocation();
        if (config.getLocationOverride() != null) {
            effectiveLocation = config.getLocationOverride();
        }

        // Rebuild the log
        final SyslogMessageLogDTO filteredSyslogMessageLogDTO = new SyslogMessageLogDTO();
        filteredSyslogMessageLogDTO.setLocation(effectiveLocation);
        filteredSyslogMessageLogDTO.setSystemId(syslogMessageLogDTO.getSystemId());
        filteredSyslogMessageLogDTO.setSourceAddress(sourceAddressOverride != null ? sourceAddressOverride : syslogMessageLogDTO.getSourceAddress());
        filteredSyslogMessageLogDTO.setSourcePort(syslogMessageLogDTO.getSourcePort());
        filteredSyslogMessageLogDTO.setMessages(messagesToForward);

        if (LOG.isDebugEnabled()) {
            final String syslogs = filteredSyslogMessageLogDTO.getMessages().stream()
                    .map(m -> new String(m.getBytes().array(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining(","));
            LOG.debug("Forwarding syslog message log from {}: {}", filteredSyslogMessageLogDTO.getSourceAddress(), syslogs);
        }

        return filteredSyslogMessageLogDTO;
    }

    @Override
    public long getNumMessagesIn(Message message) {
        final SyslogMessageLogDTO syslogMessageLogDTO = (SyslogMessageLogDTO)message;
        return syslogMessageLogDTO.getMessages().size();
    }

    @Override
    public SyslogMessageLogDTO unmarshal(String xml) {
        return super.unmarshal(MAX_MIN_THRESHOLD_XML_PATTERN.matcher(xml).replaceAll(""));
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
