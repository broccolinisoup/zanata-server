/*
 * Copyright 2016, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.zanata.notification;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * @author Alex Eng<a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Slf4j
public abstract class QueueMessageReceiver implements MessageListener {
    protected static Map<String, JmsPayloadHandler> handlers = Collections
        .emptyMap();

    public static interface JmsPayloadHandler {
        void handle(Serializable data);
    }

    protected abstract Map<String, JmsPayloadHandler> getHandlers();

    protected void addHandler(String name, JmsPayloadHandler handler) {
        synchronized (this) {
            handlers =
                ImmutableMap.<String, JmsPayloadHandler>builder()
                    .put(name, handler)
                    .build();
        }
        log.info("added handlers: {}", handlers);
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof ObjectMessage) {
            try {
                String objectType =
                    nullToEmpty(
                        message.getStringProperty(
                            NotificationManager.MessagePropertiesKey.objectType.name()));

                JmsPayloadHandler handler = getHandlers().get(objectType);
                if (handler != null) {
                    log.debug("found handler for message object type [{}]",
                        objectType);
                    handler.handle(((ObjectMessage) message).getObject());
                } else {
                    log.warn("can not find handler for message:{}", message);
                }
            } catch (JMSException e) {
                log.warn("error handling jms message: {}", message);
                Throwables.propagate(e);
            } catch (Throwable e) {
                log.warn("error handling jms message: {}", message, e);
            }
        }
    }
}
