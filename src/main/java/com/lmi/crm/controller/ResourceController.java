package com.lmi.crm.controller;

import com.lmi.crm.dto.request.UpdateResourceRequest;
import com.lmi.crm.dto.request.UploadResourceRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.ResourceResponse;
import com.lmi.crm.enums.ResourceType;
import com.lmi.crm.service.ResourceService;
import com.lmi.crm.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> uploadResource(
            @ModelAttribute @Valid UploadResourceRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        ResourceResponse response = resourceService.uploadResource(request, requestingUserId);
        return ResponseEntity.ok(ApiResponse.success("Resource uploaded successfully", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getResources(
            @RequestParam(defaultValue = "false") boolean getAll,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        Object response = resourceService.getResources(requestingUserId, getAll, type, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        ResourceResponse response = resourceService.getResourceDetail(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Resource retrieved successfully", response));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> downloadResource(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        String url = resourceService.downloadResource(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Download URL generated successfully", url));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Integer id,
            @ModelAttribute UpdateResourceRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        ResourceResponse response = resourceService.updateResource(requestingUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteResource(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        String result = resourceService.deleteResource(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }
}
