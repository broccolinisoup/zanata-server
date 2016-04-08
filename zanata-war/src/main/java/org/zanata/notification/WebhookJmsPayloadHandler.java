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

import com.google.common.base.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zanata.events.WebhookJmsEvent;
import org.zanata.service.impl.WebHooksPublisher;

import javax.inject.Named;
import java.io.Serializable;

/**
 * Handles all webhook event (WebhookJmsEvent) to fire out.
 *
 * @see WebhookJmsEvent
 * @see WebhookQueueMessageReceiver
 *
 * @author Alex Eng<a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Named("webhookJmsPayloadHandler")
@javax.enterprise.context.Dependent
@Slf4j
@NoArgsConstructor
public class WebhookJmsPayloadHandler implements
    QueueMessageReceiver.JmsPayloadHandler {

    @Override
    public void handle(Serializable data) {
        if (!(data instanceof WebhookJmsEvent)) {
            log.error("can not handle data other than type {}",
                WebhookJmsEvent.class);
            return;
        }

        WebhookJmsEvent jmsEvent = WebhookJmsEvent.class.cast(data);
        log.debug("webhook jmsEvent:{}", jmsEvent);

        WebHooksPublisher.publish(jmsEvent.getUrl(), jmsEvent.getEvent(),
            Optional.fromNullable(jmsEvent.getSecret()));
    }
}
