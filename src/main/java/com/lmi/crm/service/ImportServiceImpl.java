package com.lmi.crm.service;

import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.response.ImportResult;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AuditActionType;
import com.lmi.crm.enums.ClassificationType;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.RelatedEntityType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import com.lmi.crm.exception.RowImportException;
import com.lmi.crm.mapper.ProspectMapper;
import com.lmi.crm.util.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImportServiceImpl implements ImportService {

    private static final List<String> REQUIRED_HEADERS = List.of(
            "companyname", "city", "contactfirstname", "contactlastname");

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private ProspectLicenseeRepository prospectLicenseeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProspectMapper prospectMapper;

    @Autowired
    private AuditService auditService;

    @Override
    @Transactional
    public ImportResult importProspects(MultipartFile file, Integer requestingUserId) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Integer effectiveLicenseeId;
        Integer associateId;

        if (requestingUser.getRole().isLicenseeTier()) {
            effectiveLicenseeId = requestingUserId;
            associateId = null;
        } else if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            if (requestingUser.getLicenseeId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Associate is not linked to a licensee");
            }
            if (requestingUser.getStatus() != UserStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Associate is not active");
            }
            effectiveLicenseeId = requestingUser.getLicenseeId();
            associateId = requestingUserId;
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        int totalRows = 0;
        int imported = 0;
        List<String> errors = new ArrayList<>();
        Map<String, Integer> failuresByField = new LinkedHashMap<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File has no header row");
            }

            Map<String, Integer> headerIndex = ExcelUtil.headerIndex(headerRow);
            List<String> missingHeaders = REQUIRED_HEADERS.stream()
                    .filter(h -> !headerIndex.containsKey(h))
                    .toList();
            if (!missingHeaders.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Missing required column(s): " + String.join(", ", missingHeaders));
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (ExcelUtil.isRowEmpty(row)) {
                    continue;
                }
                totalRows++;
                int rowNumber = i + 1;
                try {
                    importRow(row, headerIndex, associateId, effectiveLicenseeId, requestingUserId);
                    imported++;
                } catch (Exception ex) {
                    errors.add("Row " + rowNumber + ": " + ex.getMessage());
                    String field = ex instanceof RowImportException rie ? rie.getField() : "other";
                    failuresByField.merge(field, 1, Integer::sum);
                }
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            log.error("Could not read import file — requestingUserId: {}", requestingUserId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not read file — ensure it is a valid Excel (.xlsx/.xls) file");
        }

        int skipped = totalRows - imported;
        String summary = buildSummary(totalRows, imported, skipped, failuresByField);

        log.info("Excel import completed — requestingUserId: {}, totalRows: {}, imported: {}, skipped: {}",
                requestingUserId, totalRows, imported, skipped);

        auditService.log(AuditActionType.PROSPECTS_IMPORTED, RelatedEntityType.SYSTEM, null, requestingUserId,
                null, null, Map.of("totalRows", totalRows, "imported", imported, "skipped", skipped));

        return ImportResult.builder()
                .totalRows(totalRows)
                .imported(imported)
                .skipped(skipped)
                .errors(errors)
                .summary(summary)
                .build();
    }

    private String buildSummary(int totalRows, int imported, int skipped, Map<String, Integer> failuresByField) {
        if (totalRows == 0) {
            return "No data rows found in the file";
        }
        if (skipped == 0) {
            return String.format("%d of %d rows imported successfully", imported, totalRows);
        }

        String breakdown = failuresByField.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> String.format("%s (%d row%s)", entry.getKey(), entry.getValue(), entry.getValue() == 1 ? "" : "s"))
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d imported, %d skipped out of %d rows.", imported, skipped, totalRows));
        if (!breakdown.isEmpty()) {
            sb.append(String.format(" Issues found — %s. See row-by-row details below.", breakdown));
        }
        return sb.toString();
    }

    private void importRow(Row row, Map<String, Integer> headerIndex, Integer associateId,
                           Integer effectiveLicenseeId, Integer requestingUserId) {

        String companyName = requireField(ExcelUtil.getString(row, headerIndex, "companyName"), "companyName");
        String city = requireField(ExcelUtil.getString(row, headerIndex, "city"), "city");
        String contactFirstName = requireField(ExcelUtil.getString(row, headerIndex, "contactFirstName"), "contactFirstName");
        String contactLastName = requireField(ExcelUtil.getString(row, headerIndex, "contactLastName"), "contactLastName");
        String designation = ExcelUtil.getString(row, headerIndex, "designation");
        String email = ExcelUtil.getString(row, headerIndex, "email");
        String phone = ExcelUtil.getString(row, headerIndex, "phone");
        String referredBy = ExcelUtil.getString(row, headerIndex, "referredBy");

        if (phone != null && !phone.matches("\\d{10}")) {
            throw new RowImportException("phone", "phone must be exactly 10 digits (no spaces/dashes): '" + phone + "'");
        }
        if (email == null && phone == null) {
            throw new RowImportException("email/phone", "at least one of email or phone is required");
        }

        String classificationRaw = ExcelUtil.getString(row, headerIndex, "classificationType");
        ClassificationType classificationType = classificationRaw == null
                ? null : parseEnum(ClassificationType.class, classificationRaw, "classificationType");

        String programRaw = ExcelUtil.getString(row, headerIndex, "programType");
        ProspectProgramType programType = null;
        if (programRaw != null) {
            programType = "O2O".equalsIgnoreCase(programRaw.trim())
                    ? ProspectProgramType.ONE_TO_ONE
                    : parseEnum(ProspectProgramType.class, programRaw, "programType");
        }

        String typeRaw = ExcelUtil.getString(row, headerIndex, "type");
        ProspectType type = typeRaw == null ? ProspectType.PROSPECT : parseEnum(ProspectType.class, typeRaw, "type");

        if (email != null) {
            prospectRepository.findByEmailIgnoreCaseAndDeletionStatusFalse(email).ifPresent(p -> {
                throw new RowImportException("email", "a prospect with email '" + email + "' already exists (id " + p.getId() + ")");
            });
        }
        if (phone != null) {
            prospectRepository.findByPhoneAndDeletionStatusFalse(phone).ifPresent(p -> {
                throw new RowImportException("phone", "a prospect with phone '" + phone + "' already exists (id " + p.getId() + ")");
            });
        }
        prospectRepository.findByContactFirstNameIgnoreCaseAndContactLastNameIgnoreCaseAndCompanyNameIgnoreCaseAndDeletionStatusFalse(
                contactFirstName, contactLastName, companyName).ifPresent(p -> {
                    throw new RowImportException("contact", "duplicate contact: " + contactFirstName + " " + contactLastName
                            + " at " + companyName + " already exists (id " + p.getId() + ")");
                });

        LocalDate entryDate = ExcelUtil.getDate(row, headerIndex, "entryDate");
        if (entryDate == null) {
            entryDate = LocalDate.now();
        }
        LocalDate firstMeetingDate = ExcelUtil.getDate(row, headerIndex, "firstMeetingDate");
        LocalDate lastMeetingDate = ExcelUtil.getDate(row, headerIndex, "lastMeetingDate");
        if (firstMeetingDate != null && lastMeetingDate != null && lastMeetingDate.isBefore(firstMeetingDate)) {
            throw new RowImportException("lastMeetingDate", "lastMeetingDate cannot be before firstMeetingDate");
        }

        Prospect prospect = Prospect.builder()
                .companyName(companyName)
                .city(city)
                .contactFirstName(contactFirstName)
                .contactLastName(contactLastName)
                .designation(designation)
                .email(email)
                .phone(phone)
                .referredBy(referredBy)
                .classificationType(classificationType)
                .programType(programType)
                .type(type)
                .associateId(associateId)
                .status(ProspectStatus.PROTECTED)
                .entryDate(entryDate)
                .firstMeetingDate(firstMeetingDate)
                .lastMeetingDate(lastMeetingDate)
                .deletionStatus(false)
                .createdBy(requestingUserId)
                .protectionPeriodMonths(prospectMapper.defaultProtectionPeriodMonths(programType))
                .build();

        Prospect saved = prospectRepository.save(prospect);
        prospectLicenseeRepository.save(prospectMapper.toProspectLicensee(saved.getId(), effectiveLicenseeId));

        auditService.log(AuditActionType.PROSPECT_CREATED, RelatedEntityType.PROSPECT, saved.getId(),
                requestingUserId, null, auditService.snapshot(saved), Map.of("source", "excel_import"));
    }

    private String requireField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new RowImportException(fieldName, fieldName + " is required");
        }
        return value;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            String allowed = String.join(", ", Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList());
            throw new RowImportException(fieldName, "invalid " + fieldName + " '" + value + "' — expected one of: " + allowed);
        }
    }
}
