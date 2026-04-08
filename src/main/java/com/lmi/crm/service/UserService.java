package com.lmi.crm.service;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.response.LicenseeResponse;

public interface UserService {

    LicenseeResponse addLicensee(AddLicenseeRequest request, Integer requestingUserId);
}
