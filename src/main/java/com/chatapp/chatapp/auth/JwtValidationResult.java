package com.chatapp.chatapp.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    public boolean isUsableEvenIfExpired() {
        return username != null && (status == ValidationStatus.VALID || status == ValidationStatus.EXPIRED);
    }
}
