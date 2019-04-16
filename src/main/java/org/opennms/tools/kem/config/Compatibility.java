/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

import com.fasterxml.jackson.annotation.JsonProperty;

public class Compatibility {
    @JsonProperty("decode-from-protobuf")
    private boolean decodeFromProtobuf = false;
    @JsonProperty("encode-in-protobuf")
    private boolean encodeInProtobuf = false;

    public boolean isDecodeFromProtobuf() {
        return decodeFromProtobuf;
    }

    public void setDecodeFromProtobuf(boolean decodeFromProtobuf) {
        this.decodeFromProtobuf = decodeFromProtobuf;
    }

    public boolean isEncodeInProtobuf() {
        return encodeInProtobuf;
    }

    public void setEncodeInProtobuf(boolean encodeInProtobuf) {
        this.encodeInProtobuf = encodeInProtobuf;
    }
}
