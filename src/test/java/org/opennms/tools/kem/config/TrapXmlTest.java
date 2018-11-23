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
import org.opennms.netmgt.trapd.TrapLogDTO;
import org.opennms.tools.kem.mirror.TrapSinkModuleMirrorer;

import com.codahale.metrics.MetricRegistry;

public class TrapXmlTest {

    @Test
    public void canUnmarshal() {

        String xml = "<trap-message-log system-id=\"cerni\n" +
                "ossmnls0301\" location=\"ONMS-AB-III\" trap-address=\"10.101.1.101\">\n" +
                "   <messages>\n" +
                "      <agent-address>10.101.1.101</agent-address>\n" +
                "      <community>publ1c</community>\n" +
                "      <version>v2</version>\n" +
                "      <timestamp>369652310</timestamp>\n" +
                "      <pdu-length>3</pdu-length>\n" +
                "      <creation-time>1542977957811</creation-time>\n" +
                "      <trap-identity generic=\"6\" specific=\"2\" enterprise-id=\".1.3.6.1.2.1.105\"/>\n" +
                "      <results>\n" +
                "         <result>\n" +
                "            <base>.1.3.6.1.2.1.105.1.3.1.1.4.1</base>\n" +
                "            <value type=\"2\">AA==</value>\n" +
                "         </result>\n" +
                "      </results>\n" +
                "   </messages>\n" +
                "</trap-message-log>\n";

        final TrapSinkModuleMirrorer mirrorer = new TrapSinkModuleMirrorer(new MetricRegistry(), new Traps());
        final TrapLogDTO trapLogDTO = mirrorer.unmarshal(xml);
        assertThat(trapLogDTO, not(nullValue()));
        assertThat(trapLogDTO.getMessages(), hasSize(1));
    }
}
