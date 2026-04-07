package com.lmi.crm.service.crud;

import com.lmi.crm.dao.ResourceRepository;
import com.lmi.crm.dto.ResourceRequestDTO;
import com.lmi.crm.dto.ResourceResponseDTO;
import com.lmi.crm.entity.Resource;
import com.lmi.crm.mapper.ResourceMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceMapper resourceMapper;

    @Override
    public ResourceResponseDTO createResource(ResourceRequestDTO request) {
        log.info("Creating new resource: {}", request.getTitle());
        Resource resource = resourceMapper.toEntity(request);
        return resourceMapper.toDTO(resourceRepository.save(resource));
    }

    @Override
    public ResourceResponseDTO getResourceById(Integer id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Resource with ID {} not found", id);
                    return new EntityNotFoundException("Resource not found with ID: " + id);
                });
        return resourceMapper.toDTO(resource);
    }

    @Override
    public List<ResourceResponseDTO> getAllResources() {
        return resourceRepository.findAll()
                .stream()
                .map(resourceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ResourceResponseDTO updateResource(Integer id, ResourceRequestDTO request) {
        log.info("Updating resource with ID {}", id);
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Resource with ID {} not found", id);
                    return new EntityNotFoundException("Resource not found with ID: " + id);
                });
        resourceMapper.updateEntity(resource, request);
        return resourceMapper.toDTO(resourceRepository.save(resource));
    }

    @Override
    public void deleteResource(Integer id) {
        log.info("Deleting resource with ID {}", id);
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Resource with ID {} not found", id);
                    return new EntityNotFoundException("Resource not found with ID: " + id);
                });
        resourceRepository.delete(resource);
    }
}
