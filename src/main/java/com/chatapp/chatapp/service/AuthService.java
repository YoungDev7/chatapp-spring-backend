package com.chatapp.chatapp.service;

import java.util.Optional;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.DTO.AuthRequest;
import com.chatapp.chatapp.DTO.AuthResponse;
import com.chatapp.chatapp.DTO.RegisterRequest;
import com.chatapp.chatapp.DTO.TokenDTO;
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
     * @return TokenDTO containing the access token and refresh token cookie
     * @throws BadCredentialsException if the provided credentials are invalid
    */
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
     * 
     * @return AuthResponse containing the new access token
     * @throws IllegalStateException if the authenticated user cannot be found in the database
     */
    public AuthResponse refreshToken() throws IllegalStateException{
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getPrincipal() instanceof User ? ((User) authentication.getPrincipal()).getEmail() : null;
        User user;

        try{
            //we want to make sure the user is in database so we cant rely on authentication.getPrincipal() directly
            user = userRepository.findUserByEmail(username).orElseThrow(() -> new IllegalStateException("user not found: " + username));
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

    private boolean isValidEmail(String email) {
        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email);
    }

}
