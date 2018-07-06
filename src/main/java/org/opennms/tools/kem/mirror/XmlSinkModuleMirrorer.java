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

package org.opennms.tools.kem.mirror;

import java.util.Objects;

import org.opennms.core.ipc.sink.api.Message;
import org.opennms.core.xml.XmlHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public abstract class XmlSinkModuleMirrorer<M extends Message> {
    private static final Logger LOG = LoggerFactory.getLogger(XmlSinkModuleMirrorer.class);

    /**
     * Handlers are not thread-safe.
     */
    private final ThreadLocal<XmlHandler<M>> xmlHandlerThreadLocal = new ThreadLocal<>();

    private final Class<M> clazz;
    protected final Meter messagesFiltered;
    protected final Meter messagesForwarded;

    public XmlSinkModuleMirrorer(MetricRegistry metrics, Class<M> clazz, String name) {
        this.clazz = Objects.requireNonNull(clazz);
        messagesFiltered = metrics.meter(name + "Filtered");
        messagesForwarded = metrics.meter(name + "Forwarded");
    }

    public abstract String getSourceTopic();

    public abstract String getTargetTopic();

    public abstract M mapIfNeedsForwarding(Message message);

    public String marshal(Message message) {
        return getXmlHandler().marshal(clazz.cast(message));
    }

    public M unmarshal(String content) {
        return getXmlHandler().unmarshal(content);
    }

    public abstract long getNumMessagesIn(Message m);

    private XmlHandler<M> getXmlHandler() {
        XmlHandler<M> xmlHandler = xmlHandlerThreadLocal.get();
        if (xmlHandler == null) {
            xmlHandler = createXmlHandler(clazz);
            xmlHandlerThreadLocal.set(xmlHandler);
        }
        return xmlHandler;
    }

    private <W> XmlHandler<W> createXmlHandler(Class<W> clazz) {
        try {
            return new XmlHandler<>(clazz);
        } catch (Throwable t) {
            // NMS-8793: This is a work-around for some failure in the Minion container
            // When invoked for the first time, the creation may fail due to
            // errors of the form "invalid protocol handler: mvn", but subsequent
            // calls always seem to work
            LOG.warn("Creating the XmlHandler failed. Retrying.", t);
            return new XmlHandler<>(clazz);
        }
    }

}
