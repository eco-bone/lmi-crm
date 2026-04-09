package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.service.NotificationService;
import com.lmi.crm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final NotificationService notificationService;
    private final UserService userService;

    public TestController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @PostMapping("/email/invite")
    public ResponseEntity<String> testInvite(@RequestParam String to) {
        notificationService.sendInviteEmail(to, "https://example.com/invite?token=TEST123", "TempPass@123");
        return ResponseEntity.ok("Invite email sent to " + to);
    }

}
