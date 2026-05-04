package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddAssociateRequest;
import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
import com.lmi.crm.dto.request.ResetPasswordRequest;
import com.lmi.crm.dto.request.UpdateUserRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.dto.response.UserResponse;
import com.lmi.crm.dto.response.UsersPageResponse;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import com.lmi.crm.service.UserService;
import com.lmi.crm.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
        log.info("POST /api/admin/create — requestingUserId: {}, newAdmin: {} {}", requestingUserId, request.getFirstName(), request.getLastName());
        UserResponse response = userService.createAdmin(request, requestingUserId);
        log.info("POST /api/admin/create — admin created — newUserId: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/licensees")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<LicenseeResponse> addLicensee(
            @Valid @RequestBody AddLicenseeRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/admin/licensees — requestingUserId: {}, newLicensee: {} {}", requestingUserId, request.getFirstName(), request.getLastName());
        LicenseeResponse response = userService.addLicensee(request, requestingUserId);
        log.info("POST /api/admin/licensees — licensee created — newUserId: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/associates")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> addAssociate(
            @Valid @RequestBody AddAssociateRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/admin/associates — requestingUserId: {}, associate: {} {}, licenseeId: {}", requestingUserId, request.getFirstName(), request.getLastName(), request.getLicenseeId());
        UserResponse response = userService.addAssociate(request, requestingUserId);
        log.info("POST /api/admin/associates — associate created — newUserId: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/licensees/associates/request")
    @PreAuthorize("hasRole('LICENSEE')")
    public ResponseEntity<String> requestAssociateCreation(
            @Valid @RequestBody RequestAssociateCreationRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/licensees/associates/request — requestingUserId: {}, associate: {} {}", requestingUserId, request.getFirstName(), request.getLastName());
        String response = userService.requestAssociateCreation(request, requestingUserId);
        log.info("POST /api/licensees/associates/request — request submitted — requestingUserId: {}", requestingUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/admin/associates/{alertId}/decision")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> approveRejectAssociateCreation(
            @PathVariable Integer alertId,
            @RequestParam boolean approve) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("PUT /api/admin/associates/{}/decision — requestingUserId: {}, approve: {}", alertId, requestingUserId, approve);
        ApiResponse<UserResponse> response = userService.approveRejectAssociateCreation(alertId, approve, requestingUserId);
        log.info("PUT /api/admin/associates/{}/decision — outcome: {} — requestingUserId: {}", alertId, approve ? "approved" : "rejected", requestingUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UsersPageResponse>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "own") String scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("GET /api/users/search — requestingUserId: {}, q: {}, scope: {}", requestingUserId, q, scope);
        UsersPageResponse response = userService.searchUsers(requestingUserId, q, scope, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", response));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('LICENSEE')")
    public ResponseEntity<ApiResponse<?>> getUsers(
            @RequestParam(defaultValue = "false") boolean getAll,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "false") boolean includeAllStatuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        Object response = userService.getUsers(requestingUserId, getAll, role, status, includeAllStatuses, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("GET /api/users/{} — requestingUserId: {}", id, requestingUserId);
        UserResponse response = userService.getUserDetail(requestingUserId, id);
        log.info("GET /api/users/{} — returned userId: {} — requestingUserId: {}", id, response.getId(), requestingUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("PUT /api/users/{}/deactivate — requestingUserId: {}", id, requestingUserId);
        UserResponse response = userService.deactivateUser(requestingUserId, id);
        log.info("PUT /api/users/{}/deactivate — deactivated — requestingUserId: {}", id, requestingUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/associates/{id}/deactivation-request")
    @PreAuthorize("hasRole('LICENSEE')")
    public ResponseEntity<String> requestAssociateDeactivation(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/users/associates/{}/deactivation-request — requestingUserId: {}", id, requestingUserId);
        String response = userService.requestAssociateDeactivation(requestingUserId, id);
        log.info("POST /api/users/associates/{}/deactivation-request — request submitted — requestingUserId: {}", id, requestingUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/associates/deactivation-requests/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> approveRejectAssociateDeactivation(
            @PathVariable Integer alertId,
            @RequestParam boolean approve) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("PUT /api/users/associates/deactivation-requests/{} — requestingUserId: {}, approve: {}", alertId, requestingUserId, approve);
        ApiResponse<UserResponse> response = userService.approveRejectAssociateDeactivation(requestingUserId, alertId, approve);
        log.info("PUT /api/users/associates/deactivation-requests/{} — outcome: {} — requestingUserId: {}", alertId, approve ? "approved" : "rejected", requestingUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> resetPassword(
            @PathVariable Integer id,
            @Valid @RequestBody ResetPasswordRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("PUT /api/users/{}/password — requestingUserId: {}", id, requestingUserId);
        String response = userService.resetPassword(requestingUserId, id, request);
        log.info("PUT /api/users/{}/password — password updated — requestingUserId: {}", id, requestingUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("PUT /api/users/{} — requestingUserId: {}", id, requestingUserId);
        UserResponse response = userService.updateUser(requestingUserId, id, request);
        log.info("PUT /api/users/{} — updated — requestingUserId: {}", id, requestingUserId);
        return ResponseEntity.ok(response);
    }
}
