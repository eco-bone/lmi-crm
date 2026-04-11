package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
import com.lmi.crm.dto.request.ResetPasswordRequest;
import com.lmi.crm.dto.request.UpdateUserRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.dto.response.UserResponse;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import com.lmi.crm.service.UserService;
import com.lmi.crm.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> createAdmin(
            @Valid @RequestBody RequestAssociateCreationRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.createAdmin(request, requestingUserId));
    }

    @PostMapping("/admin/licensees")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<LicenseeResponse> addLicensee(
            @Valid @RequestBody AddLicenseeRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.addLicensee(request, requestingUserId));
    }

    @PostMapping("/licensees/associates/request")
    @PreAuthorize("hasRole('LICENSEE')")
    public ResponseEntity<String> requestAssociateCreation(
            @Valid @RequestBody RequestAssociateCreationRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.requestAssociateCreation(request, requestingUserId));
    }

    @PutMapping("/admin/associates/{alertId}/decision")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> approveRejectAssociateCreation(
            @PathVariable Integer alertId,
            @RequestParam boolean approve) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.approveRejectAssociateCreation(alertId, approve, requestingUserId));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('LICENSEE')")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "false") boolean includeAllStatuses) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getUsers(requestingUserId, role, status, includeAllStatuses));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getUserDetail(requestingUserId, id));
    }

    @PutMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.deactivateUser(requestingUserId, id));
    }

    @PostMapping("/users/associates/{id}/deactivation-request")
    @PreAuthorize("hasRole('LICENSEE')")
    public ResponseEntity<String> requestAssociateDeactivation(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.requestAssociateDeactivation(requestingUserId, id));
    }

    @PutMapping("/users/associates/deactivation-requests/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> approveRejectAssociateDeactivation(
            @PathVariable Integer alertId,
            @RequestParam boolean approve) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.approveRejectAssociateDeactivation(requestingUserId, alertId, approve));
    }

    @PutMapping("/users/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> resetPassword(
            @PathVariable Integer id,
            @Valid @RequestBody ResetPasswordRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.resetPassword(requestingUserId, id, request));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updateUser(requestingUserId, id, request));
    }
}
