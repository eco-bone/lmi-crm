package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddGroupRequest;
import com.lmi.crm.dto.request.UpdateGroupRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.GroupResponse;
import com.lmi.crm.service.GroupService;
import com.lmi.crm.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ASSOCIATE')")
    public ResponseEntity<ApiResponse<GroupResponse>> addGroup(
            @Valid @RequestBody AddGroupRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/groups — requestingUserId: {}", requestingUserId);
        GroupResponse response = groupService.addGroup(request, requestingUserId);
        log.info("POST /api/groups — created groupId: {} — requestingUserId: {}", response.getId(), requestingUserId);
        return ResponseEntity.ok(ApiResponse.success("Group created successfully", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getGroups(
            @RequestParam(defaultValue = "false") boolean getAll,
            @RequestParam(required = false) Integer licenseeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        Object response = groupService.getGroups(requestingUserId, getAll, licenseeId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Groups retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        GroupResponse response = groupService.getGroupDetail(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Group retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ASSOCIATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateGroupRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        GroupResponse response = groupService.updateGroup(requestingUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Group updated successfully", response));
    }

    @PostMapping("/{id}/deletion-request")
    @PreAuthorize("hasRole('LICENSEE')")
    public ResponseEntity<ApiResponse<String>> requestGroupDeletion(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/groups/{}/deletion-request — requestingUserId: {}", id, requestingUserId);
        String result = groupService.requestGroupDeletion(requestingUserId, id);
        log.info("POST /api/groups/{}/deletion-request — submitted — requestingUserId: {}", id, requestingUserId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteGroup(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("DELETE /api/groups/{} — requestingUserId: {}", id, requestingUserId);
        String result = groupService.deleteGroup(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PutMapping("/deletion-requests/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> approveRejectGroupDeletion(
            @PathVariable Integer alertId,
            @RequestParam boolean approve) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("PUT /api/groups/deletion-requests/{} — requestingUserId: {}, approve: {}", alertId, requestingUserId, approve);
        return ResponseEntity.ok(groupService.approveRejectGroupDeletion(requestingUserId, alertId, approve));
    }
}
