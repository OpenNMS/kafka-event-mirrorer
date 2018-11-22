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

package org.opennms.tools.kem.eval;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.kohsuke.args4j.Option;
import org.opennms.netmgt.syslogd.api.SyslogMessageDTO;
import org.opennms.netmgt.syslogd.api.SyslogMessageLogDTO;
import org.opennms.tools.kem.Command;
import org.opennms.tools.kem.config.KemConfig;
import org.opennms.tools.kem.config.KemConfigDao;
import org.opennms.tools.kem.mirror.SyslogSinkModuleMirrorer;

import com.codahale.metrics.MetricRegistry;

public class EvalCommand implements Command {

    @Option(name = "-c", usage = "yaml configuration", metaVar = "CONFIG")
    private File configFile = Paths.get(System.getProperty("user.home"), ".kem", "config.yaml").toFile();

    @Option(name = "-s", usage = "syslog message", metaVar = "SYSLOG", required = true)
    private String syslogMessage;

    @Override
    public void execute() {
        final KemConfigDao configDao = new KemConfigDao(configFile);
        final KemConfig config = configDao.getConfig();

        if (!config.getSyslog().isEnabled()) {
            System.out.println("Syslog is disabled. Nothing to evaluate.");
            return;

        }

        final SyslogSinkModuleMirrorer mirrorer = new SyslogSinkModuleMirrorer(new MetricRegistry(), config.getSyslog());
        final SyslogMessageLogDTO syslogMessageLogDTO = new SyslogMessageLogDTO();
        final SyslogMessageDTO syslogMessageDTO = new SyslogMessageDTO();
        syslogMessageDTO.setBytes(ByteBuffer.wrap(syslogMessage.getBytes(StandardCharsets.UTF_8)));
        syslogMessageLogDTO.getMessages().add(syslogMessageDTO);

        final SyslogMessageLogDTO result = mirrorer.mapIfNeedsForwarding(syslogMessageLogDTO);
        if (result == null || result.getMessages().isEmpty()) {
            System.out.println("Message would not be mirrored");
        } else {
            System.out.println("Message would be mirrored.");
        }
    }
}
