package org.zanata.webtrans.server;

import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.zanata.service.impl.SlugEntityUpdatedListener;
import org.zanata.util.Contexts;
import org.zanata.util.ServiceLocator;

import java.util.Optional;

/**
 * Hibernate SPI. Register event listener for entity lifecycle events.
 *
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class HibernateIntegrator implements Integrator {
    @Override
    public void integrate(Configuration configuration,
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
        final EventListenerRegistry eventListenerRegistry =
                serviceRegistry.getService(EventListenerRegistry.class);

        // at the time of executing this code, weld is not started yet
        TranslationUpdateListenerLazyLoader
                updateListenerLazyLoader =
                new TranslationUpdateListenerLazyLoader();
        log.info("register event listener: {}", updateListenerLazyLoader);
        // We have to use POST_UPDATE not POST_UPDATE_COMMIT. Because we
        // still need to access some other entities to make transunit. After
        // commit the transaction is closed.
        eventListenerRegistry.appendListeners(EventType.POST_UPDATE,
                updateListenerLazyLoader);
        eventListenerRegistry.appendListeners(EventType.POST_INSERT,
                updateListenerLazyLoader);

        SlugEntityUpdatedListener slugEntityUpdatedListener =
                new SlugEntityUpdatedListener();
        eventListenerRegistry.appendListeners(EventType.POST_COMMIT_UPDATE,
                slugEntityUpdatedListener);

    }

    @Override
    public void integrate(MetadataImplementor metadata,
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
    }
}
