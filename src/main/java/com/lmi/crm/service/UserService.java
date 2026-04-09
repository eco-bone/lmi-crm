package com.lmi.crm.service;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
import com.lmi.crm.dto.request.UpdateUserRequest;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.dto.response.UserResponse;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;

import java.util.List;

public interface UserService {

    LicenseeResponse addLicensee(AddLicenseeRequest request, Integer requestingUserId);

    String requestAssociateCreation(RequestAssociateCreationRequest request, Integer requestingLicenseeId);

    UserResponse approveRejectAssociateCreation(Integer alertId, boolean approve, Integer requestingAdminId);

    List<UserResponse> getUsers(Integer requestingUserId, UserRole roleFilter, UserStatus statusFilter, boolean includeAllStatuses);

    UserResponse getUserDetail(Integer requestingUserId, Integer targetUserId);

    UserResponse updateUser(Integer requestingUserId, Integer targetUserId, UpdateUserRequest request);
}
