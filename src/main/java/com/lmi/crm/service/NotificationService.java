package com.lmi.crm.service;

public interface NotificationService {

    void sendInviteEmail(String toEmail, String inviteLink, String tempPassword);

    void sendAdminAlertEmail(String subject, String description, String alertsPageUrl);

    void sendProtectionWarningEmail(String toEmail, String prospectName, String deadline);

    void sendTaskReminderEmail(String toEmail, String taskTitle, String dueTime);

    void sendWeeklyReportEmail(String toEmail, String reportSummary);

    void sendOtpEmail(String toEmail, String otp);

    void sendUserDeactivatedEmail(String toEmail, String deactivatedUserName, String deactivatedUserRole);
}
