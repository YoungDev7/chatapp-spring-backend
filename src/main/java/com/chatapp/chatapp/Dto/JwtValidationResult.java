package com.chatapp.chatapp.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object that encapsulates the result of JWT token validation.
 * 
 * This class provides detailed information about the validation status of a JWT token,
 * including whether it's valid, expired, the extracted username, and specific validation
 * status details. It's used by the JwtService to return comprehensive validation results
 * that can be used for authentication decisions and error handling.
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JwtValidationResult {
    private boolean valid;
    private boolean expired;
    private String username;
    private ValidationStatus status;
    
    public enum ValidationStatus {
        VALID,
        EXPIRED,
        INVALID_SIGNATURE,
        MALFORMED,
        INVALID,
        ILLEGAL,
        UNSUPPORTED
    }
    
    // This method lets you know if the token can be used despite being expired
    // expiration is last thing that is checked in order. meaning that if ExpiredJwtException is thrown the token is only expired but valid 
    public boolean isUsableEvenIfExpired() {
        return username != null && (status == ValidationStatus.VALID || status == ValidationStatus.EXPIRED);
    }
}
