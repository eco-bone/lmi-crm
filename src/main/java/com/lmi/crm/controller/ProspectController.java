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
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            Object response = prospectService.getProspects(requestingUserId, getAll, type, licenseeId, associateId, page, limit);
            return ResponseEntity.ok(ApiResponse.success("Prospects retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/prospects — failed — requestingUserId: {}, getAll: {}, type: {}, licenseeId: {}, associateId: {} — {}", requestingUserId, getAll, type, licenseeId, associateId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/prospects — unexpected error — requestingUserId: {}, getAll: {}, type: {}, licenseeId: {}, associateId: {}", requestingUserId, getAll, type, licenseeId, associateId, ex);
            throw ex;
        }
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProspectsPageResponse>> searchProspects(
            @RequestParam String q,
            @RequestParam(defaultValue = "own") String scope,
            @RequestParam(required = false) ProspectType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            log.info("GET /api/prospects/search — requestingUserId: {}, q: {}, scope: {}, type: {}", requestingUserId, q, scope, type);
            ProspectsPageResponse response = prospectService.searchProspects(requestingUserId, q, scope, type, page, limit);
            return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/prospects/search — failed — requestingUserId: {}, q: {}, scope: {}, type: {} — {}", requestingUserId, q, scope, type, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/prospects/search — unexpected error — requestingUserId: {}, q: {}, scope: {}, type: {}", requestingUserId, q, scope, type, ex);
            throw ex;
        }
    }

    @GetMapping("/duplicate-check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DuplicateCheckResponse>>> checkDuplicate(
            @RequestParam String companyName) {
        try {
            log.info("GET /api/prospects/duplicate-check — companyName: {}", companyName);
            List<DuplicateCheckResponse> duplicates = prospectService.checkDuplicateProspects(companyName);
            String message = duplicates.isEmpty()
                ? "No similar prospects found"
                : String.format("Found %d similar prospect(s)", duplicates.size());
            return ResponseEntity.ok(ApiResponse.success(message, duplicates));
        } catch (RuntimeException ex) {
            log.error("GET /api/prospects/duplicate-check — failed — companyName: {} — {}", companyName, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/prospects/duplicate-check — unexpected error — companyName: {}", companyName, ex);
            throw ex;
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('LICENSEE') or hasRole('MASTER_LICENSEE') or hasRole('ASSOCIATE')")
    public ResponseEntity<ApiResponse<ProspectResponse>> addProspect(
            @Valid @RequestBody AddProspectRequest request) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
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
        } catch (RuntimeException ex) {
            log.error("POST /api/prospects — failed — requestingUserId: {}, company: {} — {}", requestingUserId, request.getCompanyName(), ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/prospects — unexpected error — requestingUserId: {}, company: {}", requestingUserId, request.getCompanyName(), ex);
            throw ex;
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProspectResponse>> getProspectDetail(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            ProspectResponse response = prospectService.getProspectDetail(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success("Prospect retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/prospects/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/prospects/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('MASTER_LICENSEE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProspectResponse>> updateProspect(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProspectRequest request) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            ProspectResponse response = prospectService.updateProspect(requestingUserId, id, request);
            return ResponseEntity.ok(ApiResponse.success("Prospect updated successfully", response));
        } catch (RuntimeException ex) {
            log.error("PUT /api/prospects/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("PUT /api/prospects/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> softDeleteProspect(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            String result = prospectService.softDeleteProspect(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success(result, null));
        } catch (RuntimeException ex) {
            log.error("DELETE /api/prospects/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("DELETE /api/prospects/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('MASTER_LICENSEE') or hasRole('ASSOCIATE')")
    public ResponseEntity<ApiResponse<String>> requestConversion(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            String result = prospectService.requestConversion(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success(result, null));
        } catch (RuntimeException ex) {
            log.error("POST /api/prospects/{}/convert — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/prospects/{}/convert — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @PutMapping("/provisional/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProspectResponse>> approveRejectProvisional(
            @PathVariable Integer alertId,
            @RequestParam ProvisionalDecision decision) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(prospectService.approveRejectProvisional(requestingUserId, alertId, decision));
        } catch (RuntimeException ex) {
            log.error("PUT /api/prospects/provisional/{} — failed — requestingUserId: {}, decision: {} — {}", alertId, requestingUserId, decision, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("PUT /api/prospects/provisional/{} — unexpected error — requestingUserId: {}, decision: {}", alertId, requestingUserId, decision, ex);
            throw ex;
        }
    }

    @PutMapping("/conversions/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProspectResponse>> approveRejectConversion(
            @PathVariable Integer alertId,
            @RequestParam boolean approve) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(prospectService.approveRejectConversion(requestingUserId, alertId, approve));
        } catch (RuntimeException ex) {
            log.error("PUT /api/prospects/conversions/{} — failed — requestingUserId: {}, approve: {} — {}", alertId, requestingUserId, approve, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("PUT /api/prospects/conversions/{} — unexpected error — requestingUserId: {}, approve: {}", alertId, requestingUserId, approve, ex);
            throw ex;
        }
    }

    @PutMapping("/extensions/{alertId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProspectResponse>> approveRejectExtension(
            @PathVariable Integer alertId,
            @RequestParam boolean approve,
            @RequestParam(required = false) Integer extensionMonths) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(prospectService.approveRejectExtension(requestingUserId, alertId, approve, extensionMonths));
        } catch (RuntimeException ex) {
            log.error("PUT /api/prospects/extensions/{} — failed — requestingUserId: {}, approve: {}, extensionMonths: {} — {}", alertId, requestingUserId, approve, extensionMonths, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("PUT /api/prospects/extensions/{} — unexpected error — requestingUserId: {}, approve: {}, extensionMonths: {}", alertId, requestingUserId, approve, extensionMonths, ex);
            throw ex;
        }
    }

    @PostMapping("/{id}/extension-request")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('MASTER_LICENSEE') or hasRole('ASSOCIATE')")
    public ResponseEntity<ApiResponse<String>> requestProtectionExtension(
            @PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            log.info("POST /api/prospects/{}/extension-request — requestingUserId: {}", id, requestingUserId);
            String response = prospectService.requestProtectionExtension(id, requestingUserId);
            log.info("POST /api/prospects/{}/extension-request — submitted — requestingUserId: {}", id, requestingUserId);
            return ResponseEntity.ok(ApiResponse.success(response, null));
        } catch (RuntimeException ex) {
            log.error("POST /api/prospects/{}/extension-request — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/prospects/{}/extension-request — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }
}
