package com.lmi.crm.service;

import com.lmi.crm.dto.response.ImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface ImportService {

    ImportResult importProspects(MultipartFile file, Integer requestingUserId);
}
