package com.lmi.crm.service;

import com.lmi.crm.dto.request.AddProspectRequest;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.enums.ProspectType;

import java.util.List;

public interface ProspectService {

    ProspectResponse addProspect(AddProspectRequest request, Integer requestingUserId);

    String requestProtectionExtension(Integer prospectId, Integer requestingUserId);

    List<ProspectResponse> getProspects(Integer requestingUserId, ProspectType typeFilter,
                                        Integer licenseeIdFilter, Integer associateIdFilter);
}
