package com.lmi.crm.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportResult {
    private int totalRows;
    private int imported;
    private int skipped;
    private List<String> errors;
}
