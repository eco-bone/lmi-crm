package com.lmi.crm.dto.response;

import com.lmi.crm.enums.ProspectStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckResponse {
    private Long id;
    private String companyName;
    private String city;
    private ProspectStatus status;
    private double similarity;
}
