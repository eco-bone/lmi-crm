package com.lmi.crm.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SendInviteRequest {

    @NotEmpty
    private List<Integer> userIds;
}
