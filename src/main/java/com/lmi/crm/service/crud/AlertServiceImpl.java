package com.lmi.crm.service.crud;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dto.AlertRequestDTO;
import com.lmi.crm.dto.AlertResponseDTO;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.mapper.AlertMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AlertServiceImpl implements AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertMapper alertMapper;

    @Override
    public AlertResponseDTO createAlert(AlertRequestDTO request) {
        log.info("Creating new alert of type {}", request.getAlertType());
        Alert alert = alertMapper.toEntity(request);
        return alertMapper.toDTO(alertRepository.save(alert));
    }

    @Override
    public AlertResponseDTO getAlertById(Integer id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Alert with ID {} not found", id);
                    return new EntityNotFoundException("Alert not found with ID: " + id);
                });
        return alertMapper.toDTO(alert);
    }

    @Override
    public List<AlertResponseDTO> getAllAlerts() {
        return alertRepository.findAll()
                .stream()
                .map(alertMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AlertResponseDTO updateAlert(Integer id, AlertRequestDTO request) {
        log.info("Updating alert with ID {}", id);
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Alert with ID {} not found", id);
                    return new EntityNotFoundException("Alert not found with ID: " + id);
                });
        alertMapper.updateEntity(alert, request);
        return alertMapper.toDTO(alertRepository.save(alert));
    }

    @Override
    public void deleteAlert(Integer id) {
        log.info("Deleting alert with ID {}", id);
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Alert with ID {} not found", id);
                    return new EntityNotFoundException("Alert not found with ID: " + id);
                });
        alertRepository.delete(alert);
    }
}
