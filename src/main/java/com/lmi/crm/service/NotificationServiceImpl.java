package com.lmi.crm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendInviteEmail(String toEmail, String inviteLink, String tempPassword) {
        log.info("Sending invite email to: {}", toEmail);
        String subject = "You've been invited to LMI CRM";
        String body = "You have been invited to join the LMI Solutions CRM portal.\n\n"
                + "Click the link below to complete your registration:\n"
                + inviteLink + "\n\n"
                + "Your temporary password is: " + tempPassword + "\n\n"
                + "Please change your password after logging in.";
        send(toEmail, subject, body);
    }

    @Override
    public void sendAdminAlertEmail(String subject, String description, String alertsPageUrl) {
        log.info("Sending admin alert email — subject: {}", subject);
        String body = "A new alert has been created in the LMI CRM portal.\n\n"
                + "Title: " + subject + "\n"
                + "Details: " + description + "\n\n"
                + "Log in to review and take action:\n"
                + alertsPageUrl;
        send(fromEmail, "New Alert: " + subject, body);
    }

    @Override
    public void sendProtectionWarningEmail(String toEmail, String prospectName, String deadline) {
        log.info("Sending protection warning email to: {} for prospect: {}", toEmail, prospectName);
        String subject = "Protection Warning: " + prospectName;
        String body = "This is a reminder that the prospect \"" + prospectName + "\" has had no recent activity "
                + "and their protection status is at risk.\n\n"
                + "Deadline: " + deadline + "\n\n"
                + "Please log a meeting or update activity before the deadline to retain protection.";
        send(toEmail, subject, body);
    }

    @Override
    public void sendTaskReminderEmail(String toEmail, String taskTitle, String dueTime) {
        log.info("Sending task reminder email to: {} for task: {}", toEmail, taskTitle);
        String subject = "Task Reminder: " + taskTitle;
        String body = "Your task \"" + taskTitle + "\" is due at " + dueTime + ".\n\n"
                + "Please log in to the LMI CRM portal to complete it.";
        send(toEmail, subject, body);
    }

    @Override
    public void sendWeeklyReportEmail(String toEmail, String reportSummary) {
        log.info("Sending weekly report email to: {}", toEmail);
        String subject = "LMI CRM — Weekly Report";
        String body = "Here is your weekly activity summary for the LMI CRM portal:\n\n"
                + reportSummary;
        send(toEmail, subject, body);
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("Sending OTP email to: {}", toEmail);
        String subject = "Your LMI CRM Verification Code";
        String body = "Your one-time verification code is: " + otp + "\n\n"
                + "This code expires in 5 minutes. Do not share it with anyone.";
        send(toEmail, subject, body);
    }

    private void send(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
