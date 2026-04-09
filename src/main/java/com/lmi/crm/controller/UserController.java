package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
import com.lmi.crm.dto.request.UpdateUserRequest;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.dto.response.UserResponse;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import com.lmi.crm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam Integer requestingUserId,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "false") boolean includeAllStatuses) {
        return ResponseEntity.ok(userService.getUsers(requestingUserId, role, status, includeAllStatuses));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserDetail(
            @RequestParam Integer requestingUserId,
            @PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserDetail(requestingUserId, id));
    }

    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable Integer id,
            @RequestParam Integer requestingUserId) {
        // TODO: replace with current user from SecurityContext
        return ResponseEntity.ok(userService.deactivateUser(requestingUserId, id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestParam Integer requestingUserId) {
        // TODO: replace with current user from SecurityContext
        return ResponseEntity.ok(userService.updateUser(requestingUserId, id, request));
    }
}
