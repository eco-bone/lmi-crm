package com.lmi.crm.service.crud;

import com.lmi.crm.dto.ResourceRequestDTO;
import com.lmi.crm.dto.ResourceResponseDTO;

import java.util.List;

public interface ResourceService {
    ResourceResponseDTO createResource(ResourceRequestDTO request);
    ResourceResponseDTO getResourceById(Integer id);
    List<ResourceResponseDTO> getAllResources();
    ResourceResponseDTO updateResource(Integer id, ResourceRequestDTO request);
    void deleteResource(Integer id);
}
