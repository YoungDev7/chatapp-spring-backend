package com.chatapp.chatapp.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService service;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest request) {
        try{
            return ResponseEntity.ok(service.authenticate(request));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(401).body("invalid email or password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(){
        return ResponseEntity.ok(service.refreshToken());
    }

    @GetMapping("/validateToken")
    public ResponseEntity<?> validateToken(){
        return ResponseEntity.ok("valid");
    }
        
}
