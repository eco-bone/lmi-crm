package com.lmi.crm.scheduler;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.RelatedEntityType;
import com.lmi.crm.service.AlertService;
import com.lmi.crm.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class ProtectionScheduler {

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private ProspectLicenseeRepository prospectLicenseeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AlertRepository alertRepository;

    // -------------------------------------------------------------------------
    // Job 2 runs before Job 1 (same cron) so that prospects reaching day 75
    // are already UNPROTECTED when Job 1 iterates, letting the status guard skip them.
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void expireFirstMeetingProtection() {
        log.info("expireFirstMeetingProtection — started");
        List<Prospect> prospects = prospectRepository.findProtectedProspectsWithNoFirstMeeting(
                LocalDate.now().minusDays(75));

        for (Prospect prospect : prospects) {
            try {
                prospect.setStatus(ProspectStatus.UNPROTECTED);
                prospectRepository.save(prospect);

                alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                        AlertType.PROSPECT_PROTECTION_WARNING, prospect.getId(), AlertStatus.PENDING)
                        .ifPresent(a -> {
                            a.setStatus(AlertStatus.RESOLVED);
                            alertRepository.save(a);
                        });

                alertService.createAlert(
                        AlertType.PROSPECT_UNPROTECTED,
                        "Prospect Unprotected — " + prospect.getCompanyName(),
                        "Prospect " + prospect.getCompanyName() + " (id: " + prospect.getId()
                                + ") has become unprotected. No first meeting was recorded within 75 days.",
                        RelatedEntityType.PROSPECT,
                        prospect.getId(),
                        null,
                        false
                );

                log.warn("Prospect unprotected (no first meeting) — prospectId: {}, company: {}",
                        prospect.getId(), prospect.getCompanyName());
            } catch (Exception e) {
                log.error("Error processing protection expiry for prospectId: {} — {}",
                        prospect.getId(), e.getMessage());
            }
        }
        log.info("expireFirstMeetingProtection — finished, processed {} prospects", prospects.size());
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkFirstMeetingDeadlines() {
        log.info("checkFirstMeetingDeadlines — started");
        List<Prospect> prospects = prospectRepository.findProtectedProspectsWithNoFirstMeeting(
                LocalDate.now().minusDays(45));

        for (Prospect prospect : prospects) {
            try {
                if (prospect.getStatus() != ProspectStatus.PROTECTED) {
                    log.debug("checkFirstMeetingDeadlines — skipping prospectId: {} (status: {})",
                            prospect.getId(), prospect.getStatus());
                    continue;
                }

                boolean warningExists = alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                        AlertType.PROSPECT_PROTECTION_WARNING, prospect.getId(), AlertStatus.PENDING)
                        .isPresent();
                if (warningExists) {
                    log.debug("checkFirstMeetingDeadlines — skipping prospectId: {} (warning already pending)",
                            prospect.getId());
                    continue;
                }

                prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospect.getId())
                        .ifPresent(pl -> {
                            userRepository.findById(pl.getLicenseeId()).ifPresent(licensee ->
                                    notificationService.sendProtectionWarningEmail(
                                            licensee.getEmail(),
                                            prospect.getCompanyName(),
                                            "Day 45 — first meeting not recorded. Prospect will become unprotected at day 75."
                                    )
                            );
                        });

                if (prospect.getAssociateId() != null) {
                    userRepository.findById(prospect.getAssociateId()).ifPresent(associate ->
                            notificationService.sendProtectionWarningEmail(
                                    associate.getEmail(),
                                    prospect.getCompanyName(),
                                    "Day 45 — first meeting not recorded. Prospect will become unprotected at day 75."
                            )
                    );
                }

                alertService.createAlert(
                        AlertType.PROSPECT_PROTECTION_WARNING,
                        "Protection Warning — " + prospect.getCompanyName(),
                        "No first meeting recorded within 45 days for prospect: " + prospect.getCompanyName()
                                + " (id: " + prospect.getId() + ")",
                        RelatedEntityType.PROSPECT,
                        prospect.getId(),
                        null,
                        false
                );

                log.warn("Protection warning fired — prospectId: {}, company: {}, entryDate: {}",
                        prospect.getId(), prospect.getCompanyName(), prospect.getEntryDate());
            } catch (Exception e) {
                log.error("Error processing first-meeting deadline for prospectId: {} — {}",
                        prospect.getId(), e.getMessage());
            }
        }
        log.info("checkFirstMeetingDeadlines — finished, processed {} prospects", prospects.size());
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void expireAfterGracePeriod() {
        log.info("expireAfterGracePeriod — started");

        List<Prospect> liExpired = prospectRepository.findInactiveProtectedProspects(
                        LocalDate.now().minusMonths(15))
                .stream().filter(p -> p.getProgramType() == ProspectProgramType.LI).toList();

        List<Prospect> siExpired = prospectRepository.findInactiveProtectedProspects(
                        LocalDate.now().minusMonths(9))
                .stream().filter(p -> p.getProgramType() == ProspectProgramType.SI).toList();

        List<Prospect> allExpired = new java.util.ArrayList<>();
        allExpired.addAll(liExpired);
        allExpired.addAll(siExpired);

        for (Prospect prospect : allExpired) {
            try {
                prospect.setStatus(ProspectStatus.UNPROTECTED);
                prospectRepository.save(prospect);

                alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                        AlertType.PROSPECT_PROTECTION_WARNING, prospect.getId(), AlertStatus.PENDING)
                        .ifPresent(a -> {
                            a.setStatus(AlertStatus.RESOLVED);
                            alertRepository.save(a);
                        });

                alertService.createAlert(
                        AlertType.PROSPECT_UNPROTECTED,
                        "Prospect Unprotected — " + prospect.getCompanyName(),
                        "Prospect " + prospect.getCompanyName() + " (id: " + prospect.getId()
                                + ") has become unprotected after grace period expired with no activity.",
                        RelatedEntityType.PROSPECT,
                        prospect.getId(),
                        null,
                        false
                );

                log.warn("Prospect unprotected (grace period expired) — prospectId: {}, company: {}, programType: {}",
                        prospect.getId(), prospect.getCompanyName(), prospect.getProgramType());
            } catch (Exception e) {
                log.error("Error processing grace period expiry for prospectId: {} — {}",
                        prospect.getId(), e.getMessage());
            }
        }
        log.info("expireAfterGracePeriod — finished, processed {} prospects", allExpired.size());
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkActivityDeadlines() {
        log.info("checkActivityDeadlines — started");

        List<Prospect> liProspects = prospectRepository.findInactiveProtectedProspects(
                        LocalDate.now().minusMonths(12))
                .stream().filter(p -> p.getProgramType() == ProspectProgramType.LI).toList();

        List<Prospect> siProspects = prospectRepository.findInactiveProtectedProspects(
                        LocalDate.now().minusMonths(6))
                .stream().filter(p -> p.getProgramType() == ProspectProgramType.SI).toList();

        List<Prospect> allInactive = new java.util.ArrayList<>();
        allInactive.addAll(liProspects);
        allInactive.addAll(siProspects);

        for (Prospect prospect : allInactive) {
            try {
                if (prospect.getStatus() != ProspectStatus.PROTECTED) {
                    log.debug("checkActivityDeadlines — skipping prospectId: {} (status: {})",
                            prospect.getId(), prospect.getStatus());
                    continue;
                }

                boolean warningExists = alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                        AlertType.PROSPECT_PROTECTION_WARNING, prospect.getId(), AlertStatus.PENDING)
                        .isPresent();
                if (warningExists) {
                    log.debug("checkActivityDeadlines — skipping prospectId: {} (warning already pending)",
                            prospect.getId());
                    continue;
                }

                prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospect.getId())
                        .ifPresent(pl -> {
                            userRepository.findById(pl.getLicenseeId()).ifPresent(licensee ->
                                    notificationService.sendProtectionWarningEmail(
                                            licensee.getEmail(),
                                            prospect.getCompanyName(),
                                            "No activity recorded within the base protection period. Grace period of 3 months has started."
                                    )
                            );
                        });

                if (prospect.getAssociateId() != null) {
                    userRepository.findById(prospect.getAssociateId()).ifPresent(associate ->
                            notificationService.sendProtectionWarningEmail(
                                    associate.getEmail(),
                                    prospect.getCompanyName(),
                                    "No activity recorded within the base protection period. Grace period of 3 months has started."
                            )
                    );
                }

                alertService.createAlert(
                        AlertType.PROSPECT_PROTECTION_WARNING,
                        "Grace Period Started — " + prospect.getCompanyName(),
                        "Prospect " + prospect.getCompanyName() + " (id: " + prospect.getId()
                                + ") has entered the grace period due to inactivity.",
                        RelatedEntityType.PROSPECT,
                        prospect.getId(),
                        null,
                        false
                );

                log.warn("Grace period started — prospectId: {}, company: {}, programType: {}, lastMeeting: {}",
                        prospect.getId(), prospect.getCompanyName(),
                        prospect.getProgramType(), prospect.getLastMeetingDate());
            } catch (Exception e) {
                log.error("Error processing activity deadline for prospectId: {} — {}",
                        prospect.getId(), e.getMessage());
            }
        }
        log.info("checkActivityDeadlines — finished, processed {} prospects", allInactive.size());
    }

    @Scheduled(cron = "0 0 8 * * MON")
    public void sendWeeklyReport() {
        log.info("Weekly report scheduler triggered");
        // TODO: implement after ReportService is built in Stage 8
    }

    @Scheduled(fixedRate = 300000)
    public void sendTaskReminders() {
        log.info("Task reminder scheduler triggered");
        // TODO: implement after TaskNoteService is built in Stage 7
    }
}
