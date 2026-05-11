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
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            ResourceResponse response = resourceService.uploadResource(request, requestingUserId);
            return ResponseEntity.ok(ApiResponse.success("Resource uploaded successfully", response));
        } catch (RuntimeException ex) {
            log.error("POST /api/resources — upload failed — requestingUserId: {} — {}", requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/resources — unexpected error during upload — requestingUserId: {}", requestingUserId, ex);
            throw ex;
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getResources(
            @RequestParam(defaultValue = "false") boolean getAll,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            Object response = resourceService.getResources(requestingUserId, getAll, type, page, limit);
            return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/resources — failed — requestingUserId: {}, getAll: {}, type: {} — {}", requestingUserId, getAll, type, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/resources — unexpected error — requestingUserId: {}, getAll: {}, type: {}", requestingUserId, getAll, type, ex);
            throw ex;
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceDetail(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            ResourceResponse response = resourceService.getResourceDetail(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success("Resource retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/resources/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/resources/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> downloadResource(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            String url = resourceService.downloadResource(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success("Download URL generated successfully", url));
        } catch (RuntimeException ex) {
            log.error("GET /api/resources/{}/download — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/resources/{}/download — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Integer id,
            @ModelAttribute UpdateResourceRequest request) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            ResourceResponse response = resourceService.updateResource(requestingUserId, id, request);
            return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", response));
        } catch (RuntimeException ex) {
            log.error("PUT /api/resources/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("PUT /api/resources/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteResource(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            String result = resourceService.deleteResource(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success(result, null));
        } catch (RuntimeException ex) {
            log.error("DELETE /api/resources/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("DELETE /api/resources/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }
}
