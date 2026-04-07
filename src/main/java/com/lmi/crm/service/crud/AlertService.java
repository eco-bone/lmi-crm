package com.lmi.crm.service.crud;

import com.lmi.crm.dto.AlertRequestDTO;
import com.lmi.crm.dto.AlertResponseDTO;

import java.util.List;

public interface AlertService {
    AlertResponseDTO createAlert(AlertRequestDTO request);
    AlertResponseDTO getAlertById(Integer id);
    List<AlertResponseDTO> getAllAlerts();
    AlertResponseDTO updateAlert(Integer id, AlertRequestDTO request);
    void deleteAlert(Integer id);
}
