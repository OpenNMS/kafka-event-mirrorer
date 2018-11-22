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

import java.util.ArrayList;
import java.util.List;

import org.opennms.netmgt.trapd.TrapIdentityDTO;
import org.snmp4j.smi.OID;

public class TrapHelper {

    /**
     * The standard traps list
     */
    private static final List<OID> GENERIC_TRAPS;

    /**
     * The dot separator in an OID
     */
    private static final char DOT_CHAR = '.';


    /**
     * Create the standard traps list - used in v2 processing
     */
    static {
        GENERIC_TRAPS = new ArrayList<>();
        GENERIC_TRAPS.add(new OID("1.3.6.1.6.3.1.1.5.1")); // coldStart
        GENERIC_TRAPS.add(new OID("1.3.6.1.6.3.1.1.5.2")); // warmStart
        GENERIC_TRAPS.add(new OID("1.3.6.1.6.3.1.1.5.3")); // linkDown
        GENERIC_TRAPS.add(new OID("1.3.6.1.6.3.1.1.5.4")); // linkUp
        GENERIC_TRAPS.add(new OID("1.3.6.1.6.3.1.1.5.5")); // authenticationFailure
        GENERIC_TRAPS.add(new OID("1.3.6.1.6.3.1.1.5.6")); // egpNeighborLoss
    }

    public static TrapIdentityDTO toTrapIdentity(String trapTypeOid) {
        OID enterpriseId = null;
        int generic;
        int specific;

        // get the last subid
        int lastIndex = trapTypeOid.lastIndexOf(TrapHelper.DOT_CHAR);
        String lastSubIdStr = trapTypeOid.substring(lastIndex + 1);
        int lastSubId = -1;
        try {
            lastSubId = Integer.parseInt(lastSubIdStr);
        } catch (NumberFormatException nfe) {
            // pass
        }

        // Check if standard trap
        if (TrapHelper.GENERIC_TRAPS.contains(new OID(trapTypeOid))) {
            // set generic
            generic = lastSubId - 1;

            // set specific to zero
            specific = 0;
        } else // not standard trap
        {
            // set generic to 6
            generic = 6;
            specific = lastSubId;

            // get the next to last subid
            int nextToLastIndex = trapTypeOid.lastIndexOf(TrapHelper.DOT_CHAR, lastIndex - 1);
            // check if value is zero
            String nextToLastSubIdStr = trapTypeOid.substring(nextToLastIndex + 1, lastIndex);
            if (nextToLastSubIdStr.equals("0")) {
                // set enterprise value to trap oid minus the
                // the last two subids
                enterpriseId = new OID(trapTypeOid.substring(0, nextToLastIndex));
            } else {
                enterpriseId = new OID(trapTypeOid.substring(0, lastIndex));
            }
        }

        final TrapIdentityDTO trapIdentityDTO = new TrapIdentityDTO();
        trapIdentityDTO.setEnterpriseId(enterpriseId != null ? enterpriseId.toDottedString() : null);
        trapIdentityDTO.setGeneric(generic);
        trapIdentityDTO.setSpecific(specific);
        return trapIdentityDTO;
    }
}
