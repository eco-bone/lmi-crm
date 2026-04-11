package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddProspectRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.service.ProspectService;
import com.lmi.crm.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/prospects")
public class ProspectController {

    private final ProspectService prospectService;

    public ProspectController(ProspectService prospectService) {
        this.prospectService = prospectService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProspectResponse>>> getProspects(
            @RequestParam(required = false) ProspectType type,
            @RequestParam(required = false) Integer licenseeId,
            @RequestParam(required = false) Integer associateId) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        List<ProspectResponse> response = prospectService.getProspects(requestingUserId, type, licenseeId, associateId);
        return ResponseEntity.ok(ApiResponse.success("Prospects retrieved successfully", response));
    }

    @PostMapping
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ASSOCIATE')")
    public ResponseEntity<ApiResponse<ProspectResponse>> addProspect(
            @Valid @RequestBody AddProspectRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/prospects — requestingUserId: {}, company: {}", requestingUserId, request.getCompanyName());
        ProspectResponse response = prospectService.addProspect(request, requestingUserId);
        log.info("POST /api/prospects — created prospectId: {} — requestingUserId: {}", response.getId(), requestingUserId);
        String message;
        if (response.getProspectStatus() == ProspectStatus.PROVISIONAL) {
            message = "Prospect has been flagged as provisional and is awaiting admin approval before further actions can be taken. Reason: " + response.getProvisionReason();
        } else {
            message = "Prospect created successfully";
        }
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/{id}/extension-request")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ASSOCIATE')")
    public ResponseEntity<ApiResponse<String>> requestProtectionExtension(
            @PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/prospects/{}/extension-request — requestingUserId: {}", id, requestingUserId);
        String response = prospectService.requestProtectionExtension(id, requestingUserId);
        log.info("POST /api/prospects/{}/extension-request — submitted — requestingUserId: {}", id, requestingUserId);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }
}
