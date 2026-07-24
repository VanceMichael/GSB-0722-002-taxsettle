package com.taxsettle.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a special deduction would violate the "one active record per
 * (taxpayer, deductionType)" invariant within a single settlement scope.
 * Extends IllegalStateException so it stays in the same failure family as the
 * existing lock guards, and maps to HTTP 409 Conflict at the REST layer.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateDeductionException extends IllegalStateException {
    public DuplicateDeductionException(String message) {
        super(message);
    }
}
