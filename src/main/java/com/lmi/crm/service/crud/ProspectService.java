package com.lmi.crm.service.crud;

import com.lmi.crm.dto.ProspectRequestDTO;
import com.lmi.crm.dto.ProspectResponseDTO;

import java.util.List;

public interface ProspectService {
    ProspectResponseDTO createProspect(ProspectRequestDTO request);
    ProspectResponseDTO getProspectById(Integer id);
    List<ProspectResponseDTO> getAllProspects();
    ProspectResponseDTO updateProspect(Integer id, ProspectRequestDTO request);
    void deleteProspect(Integer id);
}
