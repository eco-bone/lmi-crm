package com.lmi.crm.controller;

import com.lmi.crm.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/email")
public class TestEmailController {

    private final NotificationService notificationService;

    public TestEmailController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/invite")
    public ResponseEntity<String> testInvite(@RequestParam String to) {
        notificationService.sendInviteEmail(to, "https://example.com/invite?token=TEST123", "TempPass@123");
        return ResponseEntity.ok("Invite email sent to " + to);
    }

    @PostMapping("/otp")
    public ResponseEntity<String> testOtp(@RequestParam String to) {
        notificationService.sendOtpEmail(to, "847291");
        return ResponseEntity.ok("OTP email sent to " + to);
    }

    @PostMapping("/alert")
    public ResponseEntity<String> testAlert(@RequestParam String to) {
        notificationService.sendAdminAlertEmail("Test Alert Subject", "This is a test alert description.", "https://example.com/admin/alerts");
        return ResponseEntity.ok("Admin alert email sent to " + to);
    }

    @PostMapping("/protection-warning")
    public ResponseEntity<String> testProtectionWarning(@RequestParam String to) {
        notificationService.sendProtectionWarningEmail(to, "Acme Corp", "2026-05-01");
        return ResponseEntity.ok("Protection warning email sent to " + to);
    }

    @PostMapping("/task-reminder")
    public ResponseEntity<String> testTaskReminder(@RequestParam String to) {
        notificationService.sendTaskReminderEmail(to, "Follow up with prospect", "14:30");
        return ResponseEntity.ok("Task reminder email sent to " + to);
    }

    @PostMapping("/weekly-report")
    public ResponseEntity<String> testWeeklyReport(@RequestParam String to) {
        notificationService.sendWeeklyReportEmail(to, "This week: 5 new prospects, 2 conversions, 3 protection warnings.");
        return ResponseEntity.ok("Weekly report email sent to " + to);
    }
}
