package com.lmi.crm.mapper;

import com.lmi.crm.dto.ResourceRequestDTO;
import com.lmi.crm.dto.ResourceResponseDTO;
import com.lmi.crm.entity.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public Resource toEntity(ResourceRequestDTO dto) {
        Resource resource = new Resource();
        resource.setTitle(dto.getTitle());
        resource.setDescription(dto.getDescription());
        resource.setResourceType(dto.getResourceType());
        resource.setFileType(dto.getFileType());
        resource.setFileUrl(dto.getFileUrl());
        return resource;
    }

    public void updateEntity(Resource resource, ResourceRequestDTO dto) {
        resource.setTitle(dto.getTitle());
        resource.setDescription(dto.getDescription());
        resource.setResourceType(dto.getResourceType());
        resource.setFileType(dto.getFileType());
        resource.setFileUrl(dto.getFileUrl());
    }

    public ResourceResponseDTO toDTO(Resource resource) {
        ResourceResponseDTO dto = new ResourceResponseDTO();
        dto.setId(resource.getId());
        dto.setTitle(resource.getTitle());
        dto.setDescription(resource.getDescription());
        dto.setResourceType(resource.getResourceType());
        dto.setFileType(resource.getFileType());
        dto.setFileUrl(resource.getFileUrl());
        dto.setUploadedBy(resource.getUploadedBy());
        dto.setCreatedAt(resource.getCreatedAt());
        dto.setUpdatedAt(resource.getUpdatedAt());
        return dto;
    }
}
