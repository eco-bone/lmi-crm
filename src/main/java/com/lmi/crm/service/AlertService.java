package com.lmi.crm.service;

import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;

public interface AlertService {

    void createAlert(AlertType alertType, String title, String description,
                     RelatedEntityType relatedEntityType, Integer relatedEntityId,
                     Integer triggeredBy, boolean actionRequired);
}
