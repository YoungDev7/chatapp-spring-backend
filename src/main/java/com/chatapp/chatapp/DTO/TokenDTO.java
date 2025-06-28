package com.chatapp.chatapp.DTO;

import org.springframework.http.ResponseCookie;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO {
    private String accessToken;
    private ResponseCookie refreshCookie;
}
