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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Traps {
    @JsonProperty("enabled")
    private boolean enabled;
    @JsonProperty("system-id-override")
    private String systemIdOverride;
    @JsonProperty("location-override")
    private String locationOverride;
    @JsonProperty("source-topic")
    private String sourceTopic;
    @JsonProperty("target-topic")
    private String targetTopic;
    @JsonProperty("include-traps-with")
    private List<TrapCriteria> trapCriteria = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSystemIdOverride() {
        return systemIdOverride;
    }

    public void setSystemIdOverride(String systemIdOverride) {
        this.systemIdOverride = systemIdOverride;
    }

    public String getLocationOverride() {
        return locationOverride;
    }

    public void setLocationOverride(String locationOverride) {
        this.locationOverride = locationOverride;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(String sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    public String getTargetTopic() {
        return targetTopic;
    }

    public void setTargetTopic(String targetTopic) {
        this.targetTopic = targetTopic;
    }

    public List<TrapCriteria> getTrapCriteria() {
        return trapCriteria;
    }

    public void setTrapCriteria(List<TrapCriteria> trapCriteria) {
        this.trapCriteria = trapCriteria;
    }
}
