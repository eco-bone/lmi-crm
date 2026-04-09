package com.lmi.crm.service;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.dto.response.UserResponse;

public interface UserService {

    LicenseeResponse addLicensee(AddLicenseeRequest request, Integer requestingUserId);

    String requestAssociateCreation(RequestAssociateCreationRequest request, Integer requestingLicenseeId);

    UserResponse approveRejectAssociateCreation(Integer alertId, boolean approve, Integer requestingAdminId);
}
