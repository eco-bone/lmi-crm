package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.event.NotificationEvent;
import com.lmi.crm.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public TestController(UserService userService, ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/email/invite")
    public ResponseEntity<String> testInvite(@RequestParam String to) {
        try {
            eventPublisher.publishEvent(new NotificationEvent(this, "Test invite email — to: " + to,
                    ns -> ns.sendInviteEmail(to, "https://example.com/invite?token=TEST123", "TempPass@123")));
            return ResponseEntity.ok("Invite email sent to " + to);
        } catch (RuntimeException ex) {
            log.error("POST /api/test/email/invite — failed — to: {} — {}", to, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/test/email/invite — unexpected error — to: {}", to, ex);
            throw ex;
        }
    }
}
