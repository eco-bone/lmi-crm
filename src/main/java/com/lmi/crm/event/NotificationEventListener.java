package com.lmi.crm.event;

import com.lmi.crm.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationEvent(NotificationEvent event) {
        try {
            event.getAction().accept(notificationService);
        } catch (Exception e) {
            log.error("Failed to send notification — {} — {}", event.getDescription(), e.getMessage(), e);
        }
    }
}
