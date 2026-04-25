package com.lmi.crm.service;

import com.lmi.crm.dto.response.AlertResponse;
import com.lmi.crm.dto.response.AlertSummaryResponse;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;
import org.springframework.data.domain.Page;

public interface AlertService {

    void createAlert(AlertType alertType, String title, String description,
                     RelatedEntityType relatedEntityType, Integer relatedEntityId,
                     Integer triggeredBy, boolean actionRequired);

    Page<AlertResponse> getAlerts(Integer requestingUserId, AlertType typeFilter, AlertStatus statusFilter, int page, int size);

    AlertResponse getAlertDetail(Integer requestingUserId, Integer alertId);

    AlertSummaryResponse getAlertSummary(Integer requestingUserId);
}
