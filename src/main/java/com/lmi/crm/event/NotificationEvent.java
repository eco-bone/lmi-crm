package com.lmi.crm.event;

import com.lmi.crm.service.NotificationService;
import org.springframework.context.ApplicationEvent;

import java.util.function.Consumer;

/**
 * Generic event published whenever a service wants to send a notification (email/SMS).
 * The actual send is deferred until the enclosing transaction commits, so a rollback
 * after the publish call will not result in a notification being sent.
 */
public class NotificationEvent extends ApplicationEvent {

    private final String description;
    private final Consumer<NotificationService> action;

    public NotificationEvent(Object source, String description, Consumer<NotificationService> action) {
        super(source);
        this.description = description;
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public Consumer<NotificationService> getAction() {
        return action;
    }
}
