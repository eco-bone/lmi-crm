package com.lmi.crm.scheduler;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.GroupRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.entity.Group;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AlertSyncScheduler {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void syncPendingAlerts() {
        log.info("syncPendingAlerts — started");

        List<Alert> pending = alertRepository.findByStatus(AlertStatus.PENDING);
        int resolved = 0;

        for (Alert alert : pending) {
            try {
                if (shouldResolve(alert)) {
                    alert.setStatus(AlertStatus.RESOLVED);
                    alertRepository.save(alert);
                    resolved++;
                    log.info("syncPendingAlerts — auto-resolved alertId: {}, type: {}, entityId: {}",
                            alert.getId(), alert.getAlertType(), alert.getRelatedEntityId());
                }
            } catch (Exception e) {
                log.error("syncPendingAlerts — error processing alertId: {} — {}", alert.getId(), e.getMessage());
            }
        }

        log.info("syncPendingAlerts — finished — resolved: {}, skipped: {}, total: {}",
                resolved, pending.size() - resolved, pending.size());
    }

    private boolean shouldResolve(Alert alert) {
        Integer entityId = alert.getRelatedEntityId();
        if (entityId == null) return false;

        return switch (alert.getAlertType()) {

            // Provisional alert — resolve if the prospect is no longer provisional or has been deleted
            case DUPLICATE_PROSPECT -> {
                Optional<Prospect> p = prospectRepository.findById(entityId);
                yield p.isEmpty()
                        || Boolean.TRUE.equals(p.get().getDeletionStatus())
                        || p.get().getStatus() != ProspectStatus.PROVISIONAL;
            }

            // Conversion request — resolve if the prospect was converted to a client or deleted
            case PROSPECT_CONVERSION_REQUEST -> {
                Optional<Prospect> p = prospectRepository.findById(entityId);
                yield p.isEmpty()
                        || Boolean.TRUE.equals(p.get().getDeletionStatus())
                        || p.get().getType() == ProspectType.CLIENT;
            }

            // Deactivation requests — resolve if the user is already inactive or no longer exists
            case ASSOCIATE_DEACTIVATION_REQUEST, LICENSEE_DEACTIVATION_REQUEST -> {
                Optional<User> u = userRepository.findById(entityId);
                yield u.isEmpty() || u.get().getStatus() == UserStatus.INACTIVE;
            }

            // Extension request — resolve if the prospect has been deleted
            // (if extension was granted via API the alert would already be resolved inline)
            case PROTECTION_EXTENSION_REQUEST -> {
                Optional<Prospect> p = prospectRepository.findById(entityId);
                yield p.isEmpty() || Boolean.TRUE.equals(p.get().getDeletionStatus());
            }

            // Protection warning — resolve if first meeting was logged, or prospect is already unprotected/deleted
            case PROSPECT_PROTECTION_WARNING -> {
                Optional<Prospect> p = prospectRepository.findById(entityId);
                yield p.isEmpty()
                        || Boolean.TRUE.equals(p.get().getDeletionStatus())
                        || p.get().getFirstMeetingDate() != null
                        || p.get().getStatus() == ProspectStatus.UNPROTECTED;
            }

            // Unprotected alert — resolve if protection was restored or prospect was deleted
            case PROSPECT_UNPROTECTED -> {
                Optional<Prospect> p = prospectRepository.findById(entityId);
                yield p.isEmpty()
                        || Boolean.TRUE.equals(p.get().getDeletionStatus())
                        || p.get().getStatus() == ProspectStatus.PROTECTED;
            }

            // Ownership claim — resolve if the prospect no longer exists
            case OWNERSHIP_CLAIM_REQUEST -> {
                Optional<Prospect> p = prospectRepository.findById(entityId);
                yield p.isEmpty() || Boolean.TRUE.equals(p.get().getDeletionStatus());
            }

            // Group deletion request — resolve if the group has already been deleted
            case GROUP_DELETION_REQUEST -> {
                Optional<Group> g = groupRepository.findById(entityId);
                yield g.isEmpty() || Boolean.TRUE.equals(g.get().getDeletionStatus());
            }

            // Associate creation request — entityId is the requesting licensee, not the new user.
            // No reliable state to check against, skip.
            case ASSOCIATE_CREATION_REQUEST -> false;

            // Task reminders are fire-and-forget notifications, skip.
            case TASK_REMINDER -> false;
        };
    }
}
