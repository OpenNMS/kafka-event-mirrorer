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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.opennms.netmgt.syslogd.api.SyslogMessageDTO;
import org.opennms.netmgt.syslogd.api.SyslogMessageLogDTO;
import org.opennms.tools.kem.mirror.SyslogSinkModuleMirrorer;

import com.codahale.metrics.MetricRegistry;

public class SyslogMatchTest {

    @Test
    public void canMatchMessages() {
        ClassLoader classLoader = getClass().getClassLoader();
        File configFile = new File(classLoader.getResource("syslog-match-config.yaml").getFile());
        KemConfigDao dao = new KemConfigDao(configFile);
        KemConfig config = dao.getConfig();

        MetricRegistry metrics = new MetricRegistry();
        SyslogSinkModuleMirrorer mirrorer = new SyslogSinkModuleMirrorer(metrics, config.getSyslog());

        SyslogMessageLogDTO syslogMessageLogDTO = new SyslogMessageLogDTO();

        SyslogMessageDTO syslogMessageDTO = new SyslogMessageDTO();
        syslogMessageDTO.setBytes(ByteBuffer.wrap(String.format("%%CDP-4-NATIVE_VLAN_MISMATCH: Native VLAN mismatch discovered on" +
                " %s (75), with %s %s (2).", "eth0", "node2", "eth2").getBytes(StandardCharsets.UTF_8)));
        syslogMessageLogDTO.getMessages().add(syslogMessageDTO);

        syslogMessageDTO = new SyslogMessageDTO();
        syslogMessageDTO.setBytes(ByteBuffer.wrap(String.format("<189>: 2018 Apr 22 10:27:53 CDT: %%ETHPORT-5-IF_DOWN_LINK_FAILURE: Interface %s is down (Link failure)",
                "eth0").getBytes(StandardCharsets.UTF_8)));
        syslogMessageLogDTO.getMessages().add(syslogMessageDTO);

        SyslogMessageLogDTO result = mirrorer.mapIfNeedsForwarding(syslogMessageLogDTO);
        assertThat(result.getMessages(), hasSize(2));
    }
}
