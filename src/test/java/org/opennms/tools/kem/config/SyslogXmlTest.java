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

package org.opennms.tools.kem.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;
import org.opennms.netmgt.syslogd.api.SyslogMessageLogDTO;
import org.opennms.tools.kem.mirror.SyslogSinkModuleMirrorer;

import com.codahale.metrics.MetricRegistry;

public class SyslogXmlTest {

    @Test
    public void canUnmarshalMessageWithExtraFields() {
        final String xml = "<syslog-message-log source-address=\"127.0.0.1\" source-port=\"43287\" system-id=\"kc01\" location=\"KC-I\">\n" +
                "   <messages timestamp=\"2018-06-27T05:44:38.137-05:00\">YWJjMTIz</messages>\n" +
                "   <maxSyslogDropThresholdMin>0</maxSyslogDropThresholdMin>\n" +
                "   <maxSyslogIngestThresholdMin>0</maxSyslogIngestThresholdMin>\n" +
                "</syslog-message-log>\n";

        final SyslogSinkModuleMirrorer mirrorer = new SyslogSinkModuleMirrorer(new MetricRegistry(), new Syslog());
        final SyslogMessageLogDTO syslogMessageLogDTO = mirrorer.unmarshal(xml);
        assertThat(syslogMessageLogDTO, not(nullValue()));
        assertThat(syslogMessageLogDTO.getMessages(), hasSize(1));

    }
}
