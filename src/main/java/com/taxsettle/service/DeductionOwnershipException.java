package com.taxsettle.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a deduction targeted by id does not belong to the taxpayer in the
 * request path. Mapped to HTTP 404 so the endpoint never confirms or mutates
 * records owned by a different taxpayer.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DeductionOwnershipException extends RuntimeException {
    public DeductionOwnershipException(String message) {
        super(message);
    }
}
