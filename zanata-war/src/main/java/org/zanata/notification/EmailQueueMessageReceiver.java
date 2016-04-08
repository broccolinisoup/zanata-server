/*
 * Copyright 2014, Red Hat, Inc. and individual contributors as indicated by the
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

import java.util.Map;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import org.zanata.events.LanguageTeamPermissionChangedEvent;

/**
 * JMS EmailsQueue consumer. It will base on
 * org.zanata.notification.NotificationManager.MessagePropertiesKey#objectType
 * value to find a message payload handler. The objectType value is normally the
 * canonical name of the event class.
 *
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(
                propertyName = "destinationType",
                propertyValue = "javax.jms.Queue"
        ),
        @ActivationConfigProperty(
                propertyName = "destination",
                propertyValue = "jms/queue/MailsQueue"
        )
})
@Slf4j
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailQueueMessageReceiver extends QueueMessageReceiver {
    @Inject
    private LanguageTeamPermissionChangeJmsPayloadHandler languageTeamHandler;

    @Override
    protected Map<String, JmsPayloadHandler> getHandlers() {
        if (handlers.isEmpty()) {
            addHandler(LanguageTeamPermissionChangedEvent.class
                .getCanonicalName(), languageTeamHandler);
        }
        return handlers;
    }
}
