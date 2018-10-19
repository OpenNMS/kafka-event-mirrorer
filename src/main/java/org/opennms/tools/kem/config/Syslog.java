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

public class Syslog {
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
    @JsonProperty("exclude-messages-matching")
    private List<String> excludeMessagesMatching = new ArrayList<>();
    @JsonProperty("exclude-messages-containing")
    private List<String> excludeMessagesContaining = new ArrayList<>();
    @JsonProperty("include-messages-matching")
    private List<String> includeMessagesMatching = new ArrayList<>();
    @JsonProperty("include-messages-containing")
    private List<String> includeMessagesContaining = new ArrayList<>();

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

    public List<String> getExcludeMessagesMatching() {
        return excludeMessagesMatching;
    }

    public void setExcludeMessagesMatching(List<String> excludeMessagesMatching) {
        this.excludeMessagesMatching = excludeMessagesMatching;
    }

    public List<String> getExcludeMessagesContaining() {
        return excludeMessagesContaining;
    }

    public void setExcludeMessagesContaining(List<String> excludeMessagesContaining) {
        this.excludeMessagesContaining = excludeMessagesContaining;
    }

    public List<String> getIncludeMessagesMatching() {
        return includeMessagesMatching;
    }

    public void setIncludeMessagesMatching(List<String> includeMessagesMatching) {
        this.includeMessagesMatching = includeMessagesMatching;
    }

    public List<String> getIncludeMessagesContaining() {
        return includeMessagesContaining;
    }

    public void setIncludeMessagesContaining(List<String> includeMessagesContaining) {
        this.includeMessagesContaining = includeMessagesContaining;
    }
}
