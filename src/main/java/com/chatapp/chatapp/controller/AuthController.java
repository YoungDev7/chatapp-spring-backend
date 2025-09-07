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
import com.chatapp.chatapp.DTO.RegisterRequest;
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

    /**
     * Authenticates a user with email and password credentials.
     * 
     * @param request The authentication request containing email and password
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @param httpServletResponse The HTTP servlet response to set refresh token cookie
     * @return ResponseEntity containing the access token on success (200), 
     *         or error message on authentication failure (401)
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try{
            TokenDTO tokens = service.authenticate(request);
            
            httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, tokens.getRefreshCookie().toString());
            
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

    /**
     * Registers a new user account.
     * 
     * @param request The registration request containing user details (email, username, password)
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @param httpServletResponse The HTTP servlet response (unused but kept for consistency)
     * @return ResponseEntity with success message (201) on successful registration,
     *         conflict error (409) if user already exists,
     *         bad request (400) for invalid input format,
     *         or forbidden (403) for other registration failures
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        try{
            service.register(request);
            ApplicationLogger.requestLog(httpServletRequest, "user registered", request.getEmail(), 201);
            return ResponseEntity.status(201).body("registration successful");
        }catch (IllegalArgumentException e){
            String message = e.getMessage();

            if(message.equals("user with this email exists") || message.equals("user with this username exists")){
                ApplicationLogger.requestLog(httpServletRequest, "registration failed" + message, request.getEmail(), 409);
                return ResponseEntity.status(409).body(message);    
            }

            if(message.contains("Invalid email format: ") || message.equals("password or username is blank")){
                ApplicationLogger.requestLog(httpServletRequest, "registration failed" + message, request.getEmail(), 400);
                return ResponseEntity.status(400).body(message);
            }
            
            ApplicationLogger.requestLog(httpServletRequest, "registration failed" + message, request.getEmail(), 403);
            return ResponseEntity.status(403).body("registration fail");
        }
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * The refresh token is expected to be provided via HTTP-only cookie.
     * 
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @param httpServletResponse The HTTP servlet response (unused but kept for consistency)
     * @return ResponseEntity containing a new access token (200) on success,
     *         or unauthorized error (401) if refresh token is invalid or user not found
     */
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

    /**
     * Validates the current access token.
     * This endpoint can be used to check if a user's token is still valid.
     * 
     * @param httpServletRequest The HTTP servlet request for logging purposes
     * @return ResponseEntity with "valid" message (200) if token is valid.
     *         Invalid tokens will be handled by security filters before reaching this method.
     */
    @GetMapping("/validateToken")
    public ResponseEntity<?> validateToken(HttpServletRequest httpServletRequest){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";

        ApplicationLogger.requestLog(httpServletRequest, "token validated", username, 200);

        return ResponseEntity.ok("valid");
    }
        
}
