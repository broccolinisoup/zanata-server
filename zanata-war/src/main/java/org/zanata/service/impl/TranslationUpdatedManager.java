package org.zanata.service.impl;

import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.deltaspike.core.api.provider.BeanManagerProvider;
import org.zanata.ApplicationConfiguration;
import org.zanata.async.Async;
import org.zanata.common.LocaleId;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.PersonDAO;
import org.zanata.dao.TextFlowDAO;
import org.zanata.events.DocumentStatisticUpdatedEvent;
import org.zanata.events.TextFlowTargetStateEvent;
import org.zanata.events.TranslationUpdatedEvent;
import org.zanata.events.WebhookJms;
import org.zanata.events.WebhookJmsEvent;
import org.zanata.model.HDocument;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.WebHook;
import org.zanata.rest.dto.User;
import org.zanata.rest.editor.service.UserService;
import org.zanata.service.TranslationStateCache;
import org.zanata.util.UrlUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;

/**
 * Manager that handles post update of translation. Important:
 * TextFlowTargetStateEvent IS NOT asynchronous, that is why
 * DocumentStatisticUpdatedEvent is used for webhook processes.
 *
 * See {@link org.zanata.events.TextFlowTargetStateEvent}
 * See {@link org.zanata.events.DocumentStatisticUpdatedEvent}
 *
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Named("translationUpdatedManager")
@RequestScoped
@Slf4j
public class TranslationUpdatedManager {

    @Inject
    private TranslationStateCache translationStateCacheImpl;

    @Inject
    private TextFlowDAO textFlowDAO;

    @Inject
    private Event<DocumentStatisticUpdatedEvent> documentStatisticUpdatedEventEvent;

    @Inject
    private Event<WebhookJmsEvent> webhookJmsEventEvent;

    @Inject
    private DocumentDAO documentDAO;

    @Inject
    private PersonDAO personDAO;

    @Inject
    private UserService userService;

    @Inject
    private ApplicationConfiguration applicationConfiguration;

    @Inject
    private UrlUtil urlUtil;


    /**
     * This method contains all logic to be run immediately after a Text Flow
     * Target has been successfully translated.
     */
    @Async
    public void textFlowStateUpdated(
            @Observes(during = TransactionPhase.AFTER_SUCCESS)
            TextFlowTargetStateEvent event) {
        translationStateCacheImpl.textFlowStateUpdated(event);
        publishAsyncEvent(event);
        processWebHookEvent(event);
    }

    // Fire asynchronous event
    void publishAsyncEvent(TextFlowTargetStateEvent event) {
        if (BeanManagerProvider.isActive()) {
            int wordCount = textFlowDAO.getWordCount(event.getTextFlowId());

            documentStatisticUpdatedEventEvent
                    .fire(new DocumentStatisticUpdatedEvent(
                            event.getProjectIterationId(),
                            event.getDocumentId(), event.getLocaleId(),
                            wordCount,
                            event.getPreviousState(), event.getNewState()));
        }
    }

    void processWebHookEvent(TextFlowTargetStateEvent event) {
        HPerson person = personDAO.findById(event.getActorId());
        if(person == null) {
            return;
        }
        HDocument document = documentDAO.findById(event.getDocumentId());
        String docId = document.getDocId();
        String versionSlug = document.getProjectIteration().getSlug();
        HProject project = document.getProjectIteration().getProject();
        String projectSlug = project.getSlug();

        int wordCount = textFlowDAO.getWordCount(event.getTextFlowId());

        User user = userService.transferToUser(person.getAccount(),
            applicationConfiguration.isDisplayUserEmail());

        String url = urlUtil
            .fullEditorTransUnitUrl(projectSlug, versionSlug,
                event.getLocaleId(),
                LocaleId.EN_US, docId, event.getTextFlowId());

        TranslationUpdatedEvent webhookEvent =
            new TranslationUpdatedEvent(user, project.getSlug(),
                versionSlug, docId, event.getLocaleId(), url,
                event.getPreviousState(), event.getNewState(), wordCount);

        publishWebhookEvent(project.getWebHooks(), webhookEvent);
    }

    void publishWebhookEvent(List<WebHook> webHooks,
            TranslationUpdatedEvent event) {
        Map<String, String> urlSecretMap = Maps.newHashMap();

        for (WebHook webHook : webHooks) {
            urlSecretMap.put(webHook.getUrl(), webHook.getSecret());
        }
        webhookJmsEventEvent.fire(new WebhookJmsEvent(
            Lists.newArrayList(new WebhookJms(event, urlSecretMap))));
    }

    @VisibleForTesting
    public void init(TranslationStateCache translationStateCacheImpl,
        TextFlowDAO textFlowDAO,
        Event<DocumentStatisticUpdatedEvent> documentStatisticUpdatedEventEvent,
        Event<WebhookJmsEvent> webhookJmsEventEvent, DocumentDAO documentDAO,
        PersonDAO personDAO, UserService userService,
        ApplicationConfiguration applicationConfiguration, UrlUtil urlUtil) {
        this.translationStateCacheImpl = translationStateCacheImpl;
        this.textFlowDAO = textFlowDAO;
        this.documentStatisticUpdatedEventEvent =
            documentStatisticUpdatedEventEvent;
        this.webhookJmsEventEvent = webhookJmsEventEvent;
        this.documentDAO = documentDAO;
        this.personDAO = personDAO;
        this.userService = userService;
        this.applicationConfiguration = applicationConfiguration;
        this.urlUtil = urlUtil;
    }
}
