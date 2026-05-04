package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddProspectRequest;
import com.lmi.crm.dto.request.UpdateProspectRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.DuplicateCheckResponse;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.dto.response.ProspectsPageResponse;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.ProvisionalDecision;
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
    public ResponseEntity<ApiResponse<?>> getProspects(
            @RequestParam(defaultValue = "false") boolean getAll,
            @RequestParam(required = false) ProspectType type,
            @RequestParam(required = false) Integer licenseeId,
            @RequestParam(required = false) Integer associateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        Object response = prospectService.getProspects(requestingUserId, getAll, type, licenseeId, associateId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Prospects retrieved successfully", response));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProspectsPageResponse>> searchProspects(
            @RequestParam String q,
            @RequestParam(defaultValue = "own") String scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("GET /api/prospects/search — requestingUserId: {}, q: {}, scope: {}", requestingUserId, q, scope);
        ProspectsPageResponse response = prospectService.searchProspects(requestingUserId, q, scope, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", response));
    }

    @GetMapping("/duplicate-check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DuplicateCheckResponse>>> checkDuplicate(
            @RequestParam String companyName) {
        log.info("GET /api/prospects/duplicate-check — companyName: {}", companyName);
        List<DuplicateCheckResponse> duplicates = prospectService.checkDuplicateProspects(companyName);
        String message = duplicates.isEmpty()
            ? "No similar prospects found"
            : String.format("Found %d similar prospect(s)", duplicates.size());
        return ResponseEntity.ok(ApiResponse.success(message, duplicates));
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
        if (response.getStatus() == ProspectStatus.PROVISIONAL) {
            message = "Prospect has been flagged as provisional and is awaiting admin approval before further actions can be taken. Reason: " + response.getProvisionReason();
        } else {
            message = "Prospect created successfully";
        }
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProspectResponse>> getProspectDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        ProspectResponse response = prospectService.getProspectDetail(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Prospect retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProspectResponse>> updateProspect(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProspectRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        ProspectResponse response = prospectService.updateProspect(requestingUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Prospect updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> softDeleteProspect(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        String result = prospectService.softDeleteProspect(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ASSOCIATE')")
    public ResponseEntity<ApiResponse<String>> requestConversion(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        String result = prospectService.requestConversion(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PutMapping("/provisional/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProspectResponse>> approveRejectProvisional(
            @PathVariable Integer alertId,
            @RequestParam ProvisionalDecision decision) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(prospectService.approveRejectProvisional(requestingUserId, alertId, decision));
    }

    @PutMapping("/conversions/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProspectResponse>> approveRejectConversion(
            @PathVariable Integer alertId,
            @RequestParam boolean approve) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(prospectService.approveRejectConversion(requestingUserId, alertId, approve));
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
