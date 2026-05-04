package com.lmi.crm.controller;

// DEV ONLY — remove or disable before production deployment

import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.scheduler.AlertSyncScheduler;
import com.lmi.crm.scheduler.ProtectionScheduler;
import com.lmi.crm.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/scheduler")
public class SchedulerController {

    @Autowired
    private ProtectionScheduler protectionScheduler;

    @Autowired
    private AlertSyncScheduler alertSyncScheduler;

    @PostMapping("/check-first-meeting")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> checkFirstMeeting() {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("Scheduler manually triggered — job: checkFirstMeetingDeadlines, triggeredBy: {}", requestingUserId);
        protectionScheduler.checkFirstMeetingDeadlines();
        return ResponseEntity.ok(ApiResponse.success("checkFirstMeetingDeadlines triggered successfully", null));
    }

    @PostMapping("/expire-first-meeting")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> expireFirstMeeting() {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("Scheduler manually triggered — job: expireFirstMeetingProtection, triggeredBy: {}", requestingUserId);
        protectionScheduler.expireFirstMeetingProtection();
        return ResponseEntity.ok(ApiResponse.success("expireFirstMeetingProtection triggered successfully", null));
    }

    @PostMapping("/check-activity")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> checkActivity() {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("Scheduler manually triggered — job: checkActivityDeadlines, triggeredBy: {}", requestingUserId);
        protectionScheduler.checkActivityDeadlines();
        return ResponseEntity.ok(ApiResponse.success("checkActivityDeadlines triggered successfully", null));
    }

    @PostMapping("/expire-grace-period")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> expireGracePeriod() {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("Scheduler manually triggered — job: expireAfterGracePeriod, triggeredBy: {}", requestingUserId);
        protectionScheduler.expireAfterGracePeriod();
        return ResponseEntity.ok(ApiResponse.success("expireAfterGracePeriod triggered successfully", null));
    }

    @PostMapping("/sync-alerts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> syncAlerts() {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("Scheduler manually triggered — job: syncPendingAlerts, triggeredBy: {}", requestingUserId);
        alertSyncScheduler.syncPendingAlerts();
        return ResponseEntity.ok(ApiResponse.success("syncPendingAlerts triggered successfully", null));
    }
}
