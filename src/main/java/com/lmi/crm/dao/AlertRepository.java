package com.lmi.crm.dao;

import com.lmi.crm.entity.Alert;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    Optional<Alert> findByAlertTypeAndRelatedEntityIdAndStatus(AlertType alertType, Integer relatedEntityId, AlertStatus status);
}
