package com.lmi.crm.controller;

import com.lmi.crm.dto.response.AlertResponse;
import com.lmi.crm.dto.response.AlertSummaryResponse;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.service.AlertService;
import com.lmi.crm.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/alerts")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> getAlerts(
            @RequestParam(defaultValue = "false") boolean getAll,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        Object response = alertService.getAlerts(requestingUserId, getAll, type, status, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Alerts retrieved successfully", response));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AlertSummaryResponse>> getAlertSummary() {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        log.info("GET /api/admin/alerts/summary — requestingUserId: {}", requestingUserId);
        AlertSummaryResponse response = alertService.getAlertSummary(requestingUserId);
        return ResponseEntity.ok(ApiResponse.success("Alert summary retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlertDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        AlertResponse response = alertService.getAlertDetail(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Alert retrieved successfully", response));
    }
}
