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

package org.opennms.tools.kem;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;
import org.opennms.tools.kem.mirror.MirrorCommand;

/**
 * Entry point.
 */
public class App {

    @Argument(handler=SubCommandHandler.class)
    @SubCommands({
            @SubCommand(name="mirror", impl=MirrorCommand.class)
    })
    Command cmd;

    public static void main(String[] args) {
        App app = new App();
        CmdLineParser parser = new CmdLineParser(app);
        try {
            parser.parseArgument(args);
            if (app.cmd == null) {
                throw new Exception("Command required.");
            }
            app.cmd.execute();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            parser.printUsage(System.err);
        }
    }

}
