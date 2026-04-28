package com.lmi.crm.service;

import com.lmi.crm.dto.response.AlertResponse;
import com.lmi.crm.dto.response.AlertSummaryResponse;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;

public interface AlertService {

    void createAlert(AlertType alertType, String title, String description,
                     RelatedEntityType relatedEntityType, Integer relatedEntityId,
                     Integer triggeredBy, boolean actionRequired);

    Object getAlerts(Integer requestingUserId, boolean getAll, AlertType typeFilter, AlertStatus statusFilter, int page, int limit);

    AlertResponse getAlertDetail(Integer requestingUserId, Integer alertId);

    AlertSummaryResponse getAlertSummary(Integer requestingUserId);
}
