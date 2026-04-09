package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.dto.response.UserResponse;
import com.lmi.crm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/admin/licensees")
    public ResponseEntity<LicenseeResponse> addLicensee(
            @Valid @RequestBody AddLicenseeRequest request,
            @RequestParam Integer requestingUserId) {
        return ResponseEntity.ok(userService.addLicensee(request, requestingUserId));
    }

    @PostMapping("/licensees/associates/request")
    public ResponseEntity<String> requestAssociateCreation(
            @Valid @RequestBody RequestAssociateCreationRequest request,
            @RequestParam Integer requestingLicenseeId) {
        return ResponseEntity.ok(userService.requestAssociateCreation(request, requestingLicenseeId));
    }

    @PutMapping("/admin/associates/{alertId}/decision")
    public ResponseEntity<UserResponse> approveRejectAssociateCreation(
            @PathVariable Integer alertId,
            @RequestParam boolean approve,
            @RequestParam Integer requestingAdminId) {
        return ResponseEntity.ok(userService.approveRejectAssociateCreation(alertId, approve, requestingAdminId));
    }
}
