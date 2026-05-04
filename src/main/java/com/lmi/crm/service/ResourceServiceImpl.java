package com.lmi.crm.service;

import com.lmi.crm.dao.ResourceRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.UpdateResourceRequest;
import com.lmi.crm.dto.request.UploadResourceRequest;
import com.lmi.crm.dto.response.ResourceResponse;
import com.lmi.crm.dto.response.ResourcesPageResponse;
import com.lmi.crm.dto.response.ResourcesSummaryResponse;
import com.lmi.crm.entity.Resource;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.FileType;
import com.lmi.crm.enums.ResourceType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.mapper.ResourceMapper;
import com.lmi.crm.util.S3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Util s3Util;

    @Autowired
    private ResourceMapper resourceMapper;

    @Override
    @Transactional
    public ResourceResponse uploadResource(UploadResourceRequest request, Integer requestingUserId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            log.warn("uploadResource — access denied — requestingUserId: {}, role: {}", requestingUserId, requestingUser.getRole());
            throw new RuntimeException("Access denied");
        }

        FileType fileType;
        String fileUrl;

        if (request.getResourceType() == ResourceType.ZCDC) {
            validateYouTubeUrl(request.getVideoUrl());
            fileType = FileType.YOUTUBE;
            fileUrl = request.getVideoUrl();
        } else {
            MultipartFile file = request.getFile();
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is required for DOCUMENT and PPT resources");
            }
            fileType = detectFileType(file);
            validateFileTypeForResourceType(request.getResourceType(), fileType);

            String folder = request.getResourceType().name().toLowerCase();
            fileUrl = s3Util.uploadFile(file, folder);
        }

        resourceRepository.findByFileUrlAndDeletionStatusFalse(fileUrl)
                .ifPresent(existing -> {
                    throw new RuntimeException("A resource with this " +
                            (request.getResourceType() == ResourceType.ZCDC ? "YouTube URL" : "file") +
                            " already exists: " + existing.getTitle());
                });

        Resource resource = Resource.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .resourceType(request.getResourceType())
                .fileType(fileType)
                .fileUrl(fileUrl)
                .uploadedBy(requestingUserId)
                .deletionStatus(false)
                .build();

        Resource saved = resourceRepository.save(resource);
        log.info("Resource uploaded — id: {}, type: {}, fileType: {}, uploadedBy: {}",
                saved.getId(), saved.getResourceType(), saved.getFileType(), requestingUserId);

        return resourceMapper.toResponse(saved);
    }

    @Override
    public Object getResources(Integer requestingUserId, boolean getAll, ResourceType typeFilter, int page, int limit) {
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("getResources — requestingUserId: {}, getAll: {}, typeFilter: {}, page: {}, limit: {}",
                requestingUserId, getAll, typeFilter, page, limit);

        long totalCount = resourceRepository.countByDeletionStatusFalse();
        Map<ResourceType, Long> countByType = Arrays.stream(ResourceType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        type -> resourceRepository.countByResourceTypeAndDeletionStatusFalse(type)
                ));

        if (getAll) {
            Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
            Page<Resource> firstPage = typeFilter != null
                    ? resourceRepository.findByResourceTypeAndDeletionStatusFalse(typeFilter, pageable)
                    : resourceRepository.findByDeletionStatusFalse(pageable);

            return ResourcesSummaryResponse.builder()
                    .totalCount(totalCount)
                    .countByType(countByType)
                    .firstPage(firstPage.map(resourceMapper::toResponse))
                    .build();
        } else {
            Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
            Page<Resource> resources = typeFilter != null
                    ? resourceRepository.findByResourceTypeAndDeletionStatusFalse(typeFilter, pageable)
                    : resourceRepository.findByDeletionStatusFalse(pageable);

            return ResourcesPageResponse.builder()
                    .totalCount(totalCount)
                    .countByType(countByType)
                    .resources(resources.map(resourceMapper::toResponse))
                    .build();
        }
    }

    @Override
    public ResourceResponse getResourceDetail(Integer requestingUserId, Integer resourceId) {
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        if (Boolean.TRUE.equals(resource.getDeletionStatus())) {
            throw new RuntimeException("Resource not found");
        }

        return resourceMapper.toResponse(resource);
    }

    @Override
    public String downloadResource(Integer requestingUserId, Integer resourceId) {
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        if (Boolean.TRUE.equals(resource.getDeletionStatus())) {
            throw new RuntimeException("Resource not found");
        }

        log.info("Resource download — id: {}, fileType: {}, requestedBy: {}",
                resourceId, resource.getFileType(), requestingUserId);

        if (resource.getFileType() == FileType.YOUTUBE) {
            return resource.getFileUrl();
        }
        return s3Util.generatePresignedUrl(resource.getFileUrl(), 24 * 60);
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(Integer requestingUserId, Integer resourceId, UpdateResourceRequest request) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            log.warn("updateResource — access denied — requestingUserId: {}, role: {}", requestingUserId, requestingUser.getRole());
            throw new RuntimeException("Access denied");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        if (Boolean.TRUE.equals(resource.getDeletionStatus())) {
            throw new RuntimeException("Resource not found");
        }

        if (request.getTitle() != null) resource.setTitle(request.getTitle());
        if (request.getDescription() != null) resource.setDescription(request.getDescription());

        if (resource.getResourceType() == ResourceType.ZCDC && request.getVideoUrl() != null) {
            validateYouTubeUrl(request.getVideoUrl());
            resource.setFileUrl(request.getVideoUrl());
        }

        if (resource.getResourceType() != ResourceType.ZCDC
                && request.getFile() != null
                && !request.getFile().isEmpty()) {
            FileType fileType = detectFileType(request.getFile());
            validateFileTypeForResourceType(resource.getResourceType(), fileType);
            String key = s3Util.uploadFile(request.getFile(), resource.getResourceType().name().toLowerCase());
            resource.setFileUrl(key);
            resource.setFileType(fileType);
        }

        Resource saved = resourceRepository.save(resource);
        log.info("Resource updated — id: {}, updatedBy: {}", resourceId, requestingUserId);
        return resourceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public String deleteResource(Integer requestingUserId, Integer resourceId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            log.warn("deleteResource — access denied — requestingUserId: {}, role: {}", requestingUserId, requestingUser.getRole());
            throw new RuntimeException("Access denied");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        if (Boolean.TRUE.equals(resource.getDeletionStatus())) {
            throw new RuntimeException("Resource not found");
        }

        resource.setDeletionStatus(true);
        resourceRepository.save(resource);
        log.info("Resource soft deleted — id: {}, deletedBy: {}", resourceId, requestingUserId);
        return "Resource deleted successfully";
    }

    private void validateYouTubeUrl(String url) {
        if (url == null || url.isBlank())
            throw new RuntimeException("Video URL is required for ZCDC resources");

        if (!url.startsWith("https://www.youtube.com/watch?v=") &&
                !url.startsWith("https://youtu.be/") &&
                !url.startsWith("https://www.youtube.com/embed/")) {
            throw new RuntimeException("Invalid YouTube URL format. Must be a valid YouTube watch, share, or embed link");
        }

        String videoId = extractYouTubeVideoId(url);
        if (videoId == null || videoId.isBlank()) {
            throw new RuntimeException("Could not extract video ID from YouTube URL");
        }

        if (videoId.length() != 11) {
            throw new RuntimeException("Invalid YouTube video ID");
        }
    }

    private String extractYouTubeVideoId(String url) {
        if (url.startsWith("https://www.youtube.com/watch?v=")) {
            String query = url.substring("https://www.youtube.com/watch?v=".length());
            return query.split("&")[0];
        }
        if (url.startsWith("https://youtu.be/")) {
            String path = url.substring("https://youtu.be/".length());
            return path.split("\\?")[0];
        }
        if (url.startsWith("https://www.youtube.com/embed/")) {
            String path = url.substring("https://www.youtube.com/embed/".length());
            return path.split("\\?")[0];
        }
        return null;
    }

    private void validateFileTypeForResourceType(ResourceType resourceType, FileType fileType) {
        if (resourceType == ResourceType.DOCUMENT) {
            if (fileType != FileType.PDF && fileType != FileType.DOC && fileType != FileType.XLS) {
                throw new RuntimeException("DOCUMENT resources only allow PDF, DOC, DOCX, XLS, XLSX files");
            }
        } else if (resourceType == ResourceType.PPT) {
            if (fileType != FileType.PPT) {
                throw new RuntimeException("PPT resources only allow PPT, PPTX files");
            }
        }
    }

    private FileType detectFileType(MultipartFile file) {
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        if (contentType == null) throw new RuntimeException("Cannot determine file type");

        if (contentType.equals("application/pdf") || originalName.endsWith(".pdf"))
            return FileType.PDF;
        if (contentType.equals("application/msword") || originalName.endsWith(".doc"))
            return FileType.DOC;
        if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || originalName.endsWith(".docx"))
            return FileType.DOC;
        if (contentType.equals("application/vnd.ms-excel") || originalName.endsWith(".xls"))
            return FileType.XLS;
        if (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || originalName.endsWith(".xlsx"))
            return FileType.XLS;
        if (contentType.equals("application/vnd.ms-powerpoint") || originalName.endsWith(".ppt"))
            return FileType.PPT;
        if (contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                || originalName.endsWith(".pptx"))
            return FileType.PPT;

        throw new RuntimeException("Unsupported file type. Allowed: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX");
    }
}
