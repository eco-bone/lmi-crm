package com.lmi.crm.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExcelUtil {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-MMM-yyyy"));

    private ExcelUtil() {
    }

    public static Map<String, Integer> headerIndex(Row headerRow) {
        Map<String, Integer> index = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = cellString(cell);
            if (header != null) {
                index.put(normalize(header), cell.getColumnIndex());
            }
        }
        return index;
    }

    public static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            if (cellString(cell) != null) {
                return false;
            }
        }
        return true;
    }

    public static String getString(Row row, Map<String, Integer> headerIndex, String header) {
        Integer col = headerIndex.get(normalize(header));
        if (col == null) {
            return null;
        }
        return cellString(row.getCell(col));
    }

    public static LocalDate getDate(Row row, Map<String, Integer> headerIndex, String header) {
        Integer col = headerIndex.get(normalize(header));
        if (col == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String raw = cellString(cell);
        if (raw == null) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new IllegalArgumentException("Invalid date '" + raw + "' for column '" + header + "' — expected format yyyy-MM-dd");
    }

    public static String normalize(String header) {
        return header.trim().toLowerCase().replaceAll("[ _]", "");
    }

    private static String cellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        String value;
        switch (cell.getCellType()) {
            case STRING -> value = cell.getStringCellValue();
            case NUMERIC -> value = DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : stripTrailingZero(cell.getNumericCellValue());
            case BOOLEAN -> value = String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> value = cell.getCellFormula();
            default -> value = null;
        }
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static String stripTrailingZero(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
