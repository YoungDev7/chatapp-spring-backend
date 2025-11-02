package com.chatapp.chatapp.service;

import java.util.Optional;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.dto.AuthRequest;
import com.chatapp.chatapp.dto.RegisterRequest;
import com.chatapp.chatapp.dto.TokenInfo;
import com.chatapp.chatapp.entity.Token;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.TokenRepository;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    /**
     * Authenticates a user with the provided credentials and generates JWT tokens.
     * 
     * @param request the authentication request containing email and password
     * @return TokenInfo containing the access token and refresh token cookie
     * @throws BadCredentialsException if the provided credentials are invalid
    */
    public TokenInfo authenticate(AuthRequest request) throws BadCredentialsException {
        try{
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            var user = (User) authentication.getPrincipal();
            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            revokeAllUserTokens(user);
            saveUserToken(user, refreshToken);
            var refreshCookie = jwtService.createRefreshTokenCookie(refreshToken);

            return new TokenInfo(jwtToken, refreshCookie);
            
        }catch(BadCredentialsException e){
            throw e;
        }
    }

    /**
     * Registers a new user with the provided information after validating the data.
     * 
     * @param request the registration request containing username, password, and email
     * @throws IllegalArgumentException if the email already exists, username already exists,
     *                                  email format is invalid, or username/password is blank
     */
    public void register(RegisterRequest request) throws IllegalArgumentException{
        Optional<User> userOptionalEmail = userRepository.findUserByEmail(request.getEmail());
        Optional<User> userOptionalUsername = userRepository.findUserByNameIgnoreCase(request.getUsername());

        if(userOptionalEmail.isPresent()){
            throw new IllegalArgumentException("user with this email exists");
        }
        
        if(userOptionalUsername.isPresent()){
            throw new IllegalArgumentException("user with this username exists");
        }

        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format: " + request.getEmail());
        }

        if(request.getUsername().isBlank() || request.getPassword().isBlank()){
            throw new IllegalArgumentException("password or username is blank");
        }

        try{
            userService.postNewUser(new User(request.getUsername(), request.getPassword(), request.getEmail()));
        }catch(IllegalArgumentException e){
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Refreshes the access token for the currently authenticated user.
     * Performs token rotation by generating new access and refresh tokens,
     * revoking all existing valid tokens, and saving the new refresh token.
     * 
     * @return TokenInfo containing the new access token and refresh token cookie
     * @throws AuthenticationException if the authenticated user cannot be found in the database
     */
    public TokenInfo refreshToken() throws AuthenticationException{
         User user = getAuthenticatedUser();

        var newAccessToken = jwtService.generateToken(user);
        var newRefreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, newRefreshToken);
        var refreshCookie = jwtService.createRefreshTokenCookie(newRefreshToken);

        return new TokenInfo(newAccessToken, refreshCookie);
    }

    /**
     * Logs out the currently authenticated user by revoking all their valid tokens
     * and clearing the security context.
     * 
     * @throws IllegalStateException if the authenticated user cannot be found
     * @throws AuthenticationException if there is no valid authentication
     * @throws NullPointerException if required authentication data is null
     * @throws Exception for any other unexpected errors during logout
     */
    public void logout() throws IllegalStateException, NullPointerException, AuthenticationException, Exception {
        User user = getAuthenticatedUser();
        
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUid());
            
        if (validUserTokens.isEmpty()){
            return;
        }

        validUserTokens.forEach(token -> {
            token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);

        if(SecurityContextHolder.getContext().getAuthentication() != null){
            SecurityContextHolder.clearContext();
        }        
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
            .user(user)
            .token(jwtToken)
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
          token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

    private boolean isValidEmail(String email) {
        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email);
    }

    public User getAuthenticatedUser() throws AuthenticationException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new InsufficientAuthenticationException("No authentication found");
        }

        if (!(authentication.getPrincipal() instanceof User)) {
            throw new InsufficientAuthenticationException("Invalid authentication principal type");
        }

        return (User) authentication.getPrincipal();
    }

}
