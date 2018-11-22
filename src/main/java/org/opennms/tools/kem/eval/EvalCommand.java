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

import static org.opennms.tools.kem.eval.TrapHelper.toTrapIdentity;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.kohsuke.args4j.Option;
import org.opennms.netmgt.syslogd.api.SyslogMessageDTO;
import org.opennms.netmgt.syslogd.api.SyslogMessageLogDTO;
import org.opennms.netmgt.trapd.TrapDTO;
import org.opennms.netmgt.trapd.TrapIdentityDTO;
import org.opennms.netmgt.trapd.TrapLogDTO;
import org.opennms.tools.kem.Command;
import org.opennms.tools.kem.config.KemConfig;
import org.opennms.tools.kem.config.KemConfigDao;
import org.opennms.tools.kem.mirror.SyslogSinkModuleMirrorer;
import org.opennms.tools.kem.mirror.TrapSinkModuleMirrorer;

import com.codahale.metrics.MetricRegistry;

public class EvalCommand implements Command {

    @Option(name = "-c", usage = "yaml configuration", metaVar = "CONFIG")
    private File configFile = Paths.get(System.getProperty("user.home"), ".kem", "config.yaml").toFile();

    @Option(name = "-s", usage = "syslog message", metaVar = "SYSLOG")
    private String syslogMessage;

    @Option(name = "-t", usage = "trap type oid", metaVar = "TRAP")
    private String trapTypeOid;

    @Override
    public void execute() {
        final KemConfigDao configDao = new KemConfigDao(configFile);
        final KemConfig config = configDao.getConfig();

        if (syslogMessage == null && trapTypeOid == null) {
            System.out.println("No syslog message or trap type given. Nothing to check.");
            return;
        }

        if (syslogMessage != null) {
            if (config.getSyslog().isEnabled()) {
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
            } else {
                System.out.println("Syslog mirroring is disabled. Skipping eval.");
            }
        }

        if (trapTypeOid != null) {
            if (config.getTraps().isEnabled()) {
                final TrapSinkModuleMirrorer mirrorer = new TrapSinkModuleMirrorer(new MetricRegistry(), config.getTraps());
                final TrapLogDTO trapLogDTO = new TrapLogDTO();
                final TrapDTO trapDTO = new TrapDTO();
                trapDTO.setTrapIdentity(toTrapIdentity(trapTypeOid));
                trapLogDTO.getMessages().add(trapDTO);

                final TrapLogDTO result = mirrorer.mapIfNeedsForwarding(trapLogDTO);
                if (result == null || result.getMessages().isEmpty()) {
                    System.out.println("Trap would not be mirrored");
                } else {
                    System.out.println("Trap would be mirrored.");
                }
            } else {
                System.out.println("Trap mirroring is disabled. Skipping eval.");
            }
        }
    }
}
