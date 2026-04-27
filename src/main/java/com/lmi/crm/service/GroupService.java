package com.lmi.crm.service;

import com.lmi.crm.dto.request.AddGroupRequest;
import com.lmi.crm.dto.request.UpdateGroupRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.GroupResponse;

public interface GroupService {

    GroupResponse addGroup(AddGroupRequest request, Integer requestingUserId);

    Object getGroups(Integer requestingUserId, boolean getAll, Integer licenseeIdFilter, int page, int limit);

    GroupResponse getGroupDetail(Integer requestingUserId, Integer groupId);

    GroupResponse updateGroup(Integer requestingUserId, Integer groupId, UpdateGroupRequest request);

    String requestGroupDeletion(Integer requestingUserId, Integer groupId);

    String deleteGroup(Integer requestingUserId, Integer groupId);

    ApiResponse<String> approveRejectGroupDeletion(Integer requestingUserId, Integer alertId, boolean approve);
}
