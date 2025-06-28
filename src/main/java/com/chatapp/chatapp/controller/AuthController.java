package com.chatapp.chatapp.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.chatapp.DTO.AuthRequest;
import com.chatapp.chatapp.DTO.AuthResponse;
import com.chatapp.chatapp.DTO.TokenDTO;
import com.chatapp.chatapp.service.AuthService;
import com.chatapp.chatapp.util.ApplicationLogger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService service;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try{
            TokenDTO tokens = service.authenticate(request);
            
            //attach refresh cookie to response
            httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, tokens.getRefreshCookie().toString());
            
            // Return only access token in response body
            AuthResponse response = new AuthResponse(tokens.getAccessToken());
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = request.getEmail();
            
            ApplicationLogger.requestLog(httpServletRequest, "user logged in", username, 200);

            return ResponseEntity.ok(response);

        }catch (BadCredentialsException e){
            ApplicationLogger.requestLog(httpServletRequest, "user login failed", request.getEmail(), 401, e.getMessage());
            
            return ResponseEntity.status(401).body("invalid email or password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        try{
            AuthResponse response = service.refreshToken();
                        
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "unknown";
            ApplicationLogger.requestLog(httpServletRequest, "token refreshed", username, 200);

            return ResponseEntity.ok(response);

        }catch (IllegalStateException e){
            ApplicationLogger.requestLog(httpServletRequest, "user not found, how did we get here?", "unknown", 401, e.getMessage());

            return ResponseEntity.status(401).body("invalid token, user not found");
        }
    }

    @GetMapping("/validateToken")
    public ResponseEntity<?> validateToken(HttpServletRequest httpServletRequest){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";

        ApplicationLogger.requestLog(httpServletRequest, "token validated", username, 200);

        return ResponseEntity.ok("valid");
    }
        
}
