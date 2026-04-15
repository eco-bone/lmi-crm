package com.lmi.crm.controller;

import com.lmi.crm.dto.response.AlertResponse;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.service.AlertService;
import com.lmi.crm.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<ApiResponse<Page<AlertResponse>>> getAlerts(
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        Page<AlertResponse> response = alertService.getAlerts(requestingUserId, type, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Alerts retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlertDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        AlertResponse response = alertService.getAlertDetail(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Alert retrieved successfully", response));
    }
}
