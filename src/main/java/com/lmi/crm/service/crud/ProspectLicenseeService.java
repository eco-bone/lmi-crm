package com.lmi.crm.service.crud;

import com.lmi.crm.dto.ProspectLicenseeRequestDTO;
import com.lmi.crm.dto.ProspectLicenseeResponseDTO;

import java.util.List;

public interface ProspectLicenseeService {
    ProspectLicenseeResponseDTO createProspectLicensee(ProspectLicenseeRequestDTO request);
    ProspectLicenseeResponseDTO getProspectLicenseeById(Integer id);
    List<ProspectLicenseeResponseDTO> getAllProspectLicensees();
    ProspectLicenseeResponseDTO updateProspectLicensee(Integer id, ProspectLicenseeRequestDTO request);
    void deleteProspectLicensee(Integer id);
}
