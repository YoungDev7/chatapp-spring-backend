package com.chatapp.chatapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.chatapp.dto.AuthRequest;
import com.chatapp.chatapp.dto.AuthResponse;
import com.chatapp.chatapp.dto.RegisterRequest;
import com.chatapp.chatapp.dto.TokenInfo;
import com.chatapp.chatapp.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for handling authentication and authorization operations.
 * 
 * <p>
 * This controller provides endpoints for user authentication, registration,
 * token management, and logout functionality. It handles JWT token generation,
 * refresh token rotation, and session management.
 * </p>
 * 
 * <p>
 * Base path: {@code /api/v1/auth}
 * </p>
 * 
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>User authentication with email/password</li>
 * <li>New user registration</li>
 * <li>JWT access token and refresh token management</li>
 * <li>Token validation and refresh</li>
 * <li>User logout with token revocation</li>
 * </ul>
 * 
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Authenticates a user with email and password credentials.
     * 
     * <p>
     * HTTP Method: POST
     * </p>
     * <p>
     * Path: {@code /api/v1/auth/authenticate}
     * </p>
     * 
     * <p>
     * On successful authentication, returns an access token in the response body
     * and sets a refresh token as an HTTP-only cookie. The access token should be
     * included in subsequent requests via the Authorization header.
     * </p>
     * 
     * <p>
     * Request body format:
     * </p>
     * 
     * <pre>
     * {
     *   "email": "user@example.com",
     *   "password": "password123"
     * }
     * </pre>
     * 
     * <p>
     * Success response (200 OK):
     * </p>
     * 
     * <pre>
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIs..."
     * }
     * </pre>
     * 
     * @param request             The authentication request containing email and
     *                            password credentials
     * @param httpServletRequest  The HTTP servlet request for logging and context
     * @param httpServletResponse The HTTP servlet response to set the refresh token
     *                            cookie
     * @return ResponseEntity containing the AuthResponse with access token on
     *         success (200 OK),
     *         or error message string on authentication failure (401 Unauthorized)
     * @throws BadCredentialsException if the provided credentials are invalid
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest request, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        try {
            TokenInfo tokens = authService.authenticate(request);

            httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, tokens.getRefreshCookie().toString());

            AuthResponse response = new AuthResponse(tokens.getAccessToken());

            log.info("[{}] user logged in", 200);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("[{}] user login failed: {}", 401, e.getMessage());

            return ResponseEntity.status(401).body("invalid email or password");
        }
    }

    /**
     * Registers a new user account in the system.
     * 
     * <p>
     * HTTP Method: POST
     * </p>
     * <p>
     * Path: {@code /api/v1/auth/register}
     * </p>
     * 
     * <p>
     * Creates a new user account with the provided credentials. Email and username
     * must be unique. Password and username cannot be blank, and email must be in
     * valid format.
     * </p>
     * 
     * <p>
     * Request body format:
     * </p>
     * 
     * <pre>
     * {
     *   "email": "user@example.com",
     *   "username": "johndoe",
     *   "password": "securePassword123"
     * }
     * </pre>
     * 
     * <p>
     * Response codes:
     * </p>
     * <ul>
     * <li>201 Created: Registration successful</li>
     * <li>400 Bad Request: Invalid email format or blank username/password</li>
     * <li>409 Conflict: User with email or username already exists</li>
     * <li>403 Forbidden: Other registration failures</li>
     * </ul>
     * 
     * @param request             The registration request containing email,
     *                            username, and password
     * @param httpServletRequest  The HTTP servlet request for logging and context
     * @param httpServletResponse The HTTP servlet response (reserved for future
     *                            use)
     * @return ResponseEntity with success message (201 Created) on successful
     *         registration,
     *         conflict error message (409 Conflict) if user already exists,
     *         bad request error message (400 Bad Request) for invalid input format,
     *         or forbidden error message (403 Forbidden) for other registration
     *         failures
     * @throws IllegalArgumentException if the registration request contains invalid
     *                                  data
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        try {
            authService.register(request);
            log.info("[{}] user registered", 201);
            return ResponseEntity.status(201).body("registration successful");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();

            if (message.equals("user with this email exists") || message.equals("user with this username exists")) {
                log.warn("[{}] registration failed: {}", 409, message);
                return ResponseEntity.status(409).body(message);
            }

            if (message.contains("Invalid email format: ") || message.equals("password or username is blank")) {
                log.warn("[{}] registration failed: {}", 400, message);
                return ResponseEntity.status(400).body(message);
            }

            log.warn("[{}] registration failed: {}", 403, message);
            return ResponseEntity.status(403).body("registration fail");
        }
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * 
     * <p>
     * HTTP Method: POST
     * </p>
     * <p>
     * Path: {@code /api/v1/auth/refresh}
     * </p>
     * 
     * <p>
     * Implements token rotation security by generating new access and refresh
     * tokens
     * while revoking all existing valid tokens for the user. The refresh token must
     * be
     * provided via an HTTP-only cookie in the request. Upon success, a new access
     * token
     * is returned in the response body and a new refresh token is set as a cookie.
     * </p>
     * 
     * <p>
     * Success response (200 OK):
     * </p>
     * 
     * <pre>
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIs..."
     * }
     * </pre>
     * 
     * <p>
     * This endpoint requires a valid refresh token cookie from a previous
     * authentication.
     * </p>
     * 
     * @param httpServletRequest  The HTTP servlet request containing the refresh
     *                            token cookie
     * @param httpServletResponse The HTTP servlet response to set the new refresh
     *                            token cookie
     * @return ResponseEntity containing AuthResponse with a new access token (200
     *         OK) on success,
     *         or unauthorized error message (401 Unauthorized) if refresh token is
     *         invalid,
     *         expired, or user not found
     * @throws Exception if there's an error during token refresh or user retrieval
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            TokenInfo tokenInfo = authService.refreshToken();
            httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, tokenInfo.getRefreshCookie().toString());
            AuthResponse response = new AuthResponse(tokenInfo.getAccessToken());

            log.info("[{}] token refreshed", 200);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("[{}] Refresh error: {}", 401, e.getMessage());
            return ResponseEntity.status(401).body("Refresh error");
        }
    }

    /**
     * Validates the current access token from the request.
     * 
     * <p>
     * HTTP Method: GET
     * </p>
     * <p>
     * Path: {@code /api/v1/auth/validateToken}
     * </p>
     * 
     * <p>
     * This endpoint can be used by clients to verify if their current access token
     * is still valid and properly authenticated. The token should be provided in
     * the
     * Authorization header as a Bearer token.
     * </p>
     * 
     * <p>
     * Note: Invalid or expired tokens will be rejected by security filters before
     * reaching this method, resulting in a 401 Unauthorized response from the
     * filter chain.
     * </p>
     * 
     * <p>
     * Success response (200 OK): "valid"
     * </p>
     * 
     * @param httpServletRequest The HTTP servlet request for logging and context
     * @return ResponseEntity with "valid" string message (200 OK) if the token is
     *         authenticated.
     *         Unauthenticated tokens are handled by security filters and do not
     *         reach this method.
     */
    @GetMapping("/validateToken")
    public ResponseEntity<?> validateToken(HttpServletRequest httpServletRequest) {
        log.info("[{}] token validated", 200);

        return ResponseEntity.ok("valid");
    }

    /**
     * Logs out the currently authenticated user from the system.
     * 
     * <p>
     * HTTP Method: POST
     * </p>
     * <p>
     * Path: {@code /api/v1/auth/logout}
     * </p>
     * 
     * <p>
     * Performs a complete logout by revoking all valid access and refresh tokens
     * associated with the user's session and clearing the security context. This
     * ensures
     * that all tokens are invalidated and cannot be used for future requests.
     * </p>
     * 
     * <p>
     * After logout, the client should discard their access token and refresh token
     * cookie. The user will need to authenticate again to obtain new tokens.
     * </p>
     * 
     * <p>
     * Success response (200 OK): "logout successful"
     * </p>
     * 
     * @param httpServletRequest The HTTP servlet request for logging and context
     * @return ResponseEntity with success message (200 OK) on successful logout,
     *         or unauthorized error message (401 Unauthorized) if logout fails due
     *         to
     *         authentication issues or if the user is not properly authenticated
     * @throws Exception if there's an error during the logout process
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpServletRequest) {
        try {
            authService.logout();
            log.info("[{}] logout successful", 200);
            return ResponseEntity.ok("logout successful");
        } catch (Exception e) {
            log.warn("[{}] Logout failed: {}", HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("logout failed");
        }
    }

}
