package com.chatapp.chatapp.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.DTO.AuthRequest;
import com.chatapp.chatapp.DTO.AuthResponse;
import com.chatapp.chatapp.DTO.TokenDTO;
import com.chatapp.chatapp.entity.Token;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.IUserRepository;
import com.chatapp.chatapp.repository.TokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final IUserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public TokenDTO authenticate(AuthRequest request) throws BadCredentialsException {
        try{
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            var user = (User) authentication.getPrincipal();
            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            revokeAllUserTokens(user);
            saveUserToken(user, refreshToken);
            var refreshCookie = jwtService.createRefreshTokenCookie(refreshToken);

            return new TokenDTO(jwtToken, refreshCookie);
            
        }catch(BadCredentialsException e){
            throw e;
        }
    }

    public AuthResponse refreshToken() throws IllegalStateException{
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getPrincipal() instanceof User ? ((User) authentication.getPrincipal()).getEmail() : null;
        User user;

        try{
            //we want to make sure the user is in database so we cant rely on authentication.getPrincipal() directly
            user = repository.findUserByEmail(username).orElseThrow(() -> new IllegalStateException("user not found: " + username));
        }catch (IllegalStateException e){
            throw e;
        }

        var newAccessToken = jwtService.generateToken(user);

        return new AuthResponse(newAccessToken);
    }

    //TODO: there is no logic that changes the expired status when the token expires
    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
            .user(user)
            .token(jwtToken)
            .expired(false)
            .revoked(false)
            .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUid());
        
        if (validUserTokens.isEmpty()){
            return;
        }

        validUserTokens.forEach(token -> {
          token.setExpired(true);
          token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

}
