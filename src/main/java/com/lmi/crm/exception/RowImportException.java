package com.lmi.crm.exception;

import lombok.Getter;

@Getter
public class RowImportException extends RuntimeException {

    private final String field;

    public RowImportException(String field, String message) {
        super(message);
        this.field = field;
    }
}
