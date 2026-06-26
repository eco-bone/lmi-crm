package com.lmi.crm.event;

import com.lmi.crm.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.*;

/**
 * Verifies that NotificationEvent is only delivered to NotificationService after the
 * publishing transaction commits, and is dropped entirely on rollback.
 */
class NotificationEventListenerTest {

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        NotificationService notificationService() {
            return mock(NotificationService.class);
        }

        @Bean
        NotificationEventListener notificationEventListener() {
            return new NotificationEventListener();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void emailIsSentWhenTransactionCommits() {
        NotificationService notificationService = context.getBean(NotificationService.class);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        tx.execute(status -> {
            context.publishEvent(new NotificationEvent(this, "commit-case",
                    ns -> ns.sendOtpEmail("commit@example.com", "111111")));
            return null;
        });

        verify(notificationService, times(1)).sendOtpEmail("commit@example.com", "111111");
    }

    @Test
    void emailIsNotSentWhenTransactionRollsBack() {
        NotificationService notificationService = context.getBean(NotificationService.class);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        try {
            tx.execute(status -> {
                context.publishEvent(new NotificationEvent(this, "rollback-case",
                        ns -> ns.sendOtpEmail("rollback@example.com", "222222")));
                throw new RuntimeException("simulated failure after publish");
            });
        } catch (RuntimeException expected) {
            // expected — transaction should roll back
        }

        verify(notificationService, never()).sendOtpEmail(eq("rollback@example.com"), anyString());
    }
}
