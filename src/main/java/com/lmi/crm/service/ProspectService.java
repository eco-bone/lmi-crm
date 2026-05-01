package com.lmi.crm.service;

import com.lmi.crm.dto.request.AddProspectRequest;
import com.lmi.crm.dto.request.UpdateProspectRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.DuplicateCheckResponse;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.ProvisionalDecision;

import java.util.List;


public interface ProspectService {

    ProspectResponse addProspect(AddProspectRequest request, Integer requestingUserId);

    String requestProtectionExtension(Integer prospectId, Integer requestingUserId);

    Object getProspects(Integer requestingUserId, boolean getAll, ProspectType typeFilter,
                        Integer licenseeIdFilter, Integer associateIdFilter, int page, int limit);

    ProspectResponse getProspectDetail(Integer requestingUserId, Integer prospectId);

    ProspectResponse updateProspect(Integer requestingUserId, Integer prospectId, UpdateProspectRequest request);

    String softDeleteProspect(Integer requestingUserId, Integer prospectId);

    String requestConversion(Integer requestingUserId, Integer prospectId);

    ApiResponse<ProspectResponse> approveRejectConversion(Integer requestingUserId, Integer alertId, boolean approve);

    ApiResponse<ProspectResponse> approveRejectProvisional(Integer requestingUserId, Integer alertId, ProvisionalDecision decision);

    List<DuplicateCheckResponse> checkDuplicateProspects(String companyName);
}
