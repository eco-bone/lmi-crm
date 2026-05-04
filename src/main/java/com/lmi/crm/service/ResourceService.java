package com.lmi.crm.service;

import com.lmi.crm.dto.request.UpdateResourceRequest;
import com.lmi.crm.dto.request.UploadResourceRequest;
import com.lmi.crm.dto.response.ResourceResponse;
import com.lmi.crm.enums.ResourceType;

public interface ResourceService {

    ResourceResponse uploadResource(UploadResourceRequest request, Integer requestingUserId);

    Object getResources(Integer requestingUserId, boolean getAll, ResourceType typeFilter, int page, int limit);

    ResourceResponse getResourceDetail(Integer requestingUserId, Integer resourceId);

    String downloadResource(Integer requestingUserId, Integer resourceId);

    ResourceResponse updateResource(Integer requestingUserId, Integer resourceId, UpdateResourceRequest request);

    String deleteResource(Integer requestingUserId, Integer resourceId);
}
