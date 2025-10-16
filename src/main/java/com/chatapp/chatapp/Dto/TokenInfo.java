package com.chatapp.chatapp.Dto;

import org.springframework.http.ResponseCookie;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenInfo {
    private String accessToken;
    private ResponseCookie refreshCookie;
}
