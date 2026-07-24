package com.taxsettle.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a deduction references a family member that does not belong to the
 * taxpayer in the request path. Mapped to HTTP 404 so the endpoint never
 * confirms the existence of another taxpayer's family member.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class FamilyMemberOwnershipException extends RuntimeException {
    public FamilyMemberOwnershipException(String message) {
        super(message);
    }
}
