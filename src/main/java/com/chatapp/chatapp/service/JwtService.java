package com.chatapp.chatapp.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.dto.JwtValidationResult;
import com.chatapp.chatapp.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Service responsible for JWT (JSON Web Token) operations.
 * 
 * <p>
 * This service handles the complete JWT lifecycle including token generation,
 * validation, and cookie management. It provides secure token-based
 * authentication
 * using HMAC-SHA256 signing algorithm and manages both access tokens and
 * refresh tokens.
 * 
 * <p>
 * Key responsibilities include:
 * <ul>
 * <li>Generating access tokens with user claims</li>
 * <li>Generating refresh tokens for token rotation</li>
 * <li>Validating JWT tokens and handling various error cases</li>
 * <li>Creating secure HTTP-only refresh token cookies</li>
 * <li>Extracting and parsing JWT claims</li>
 * </ul>
 * 
 * <p>
 * <b>ATTENTION:</b> When refactoring or updating this class, ensure that
 * changes
 * are reflected in MockJwtService.java. Both files need to be in sync for
 * accurate
 * testing purposes.
 */
@Service
public class JwtService {

  @Value("${application.security.jwt.secret-key}")
  private String secretKey;
  @Value("${application.security.jwt.expiration}")
  private long jwtExpiration;
  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshExpiration;

  /**
   * Creates a secure HTTP-only cookie containing the refresh token.
   * 
   * <p>
   * The cookie is configured with the following security settings:
   * <ul>
   * <li>HttpOnly: prevents client-side JavaScript access</li>
   * <li>Secure: ensures transmission only over HTTPS</li>
   * <li>SameSite=Strict: protects against CSRF attacks</li>
   * <li>Path=/api/v1/auth/refresh: limits cookie scope to refresh endpoint</li>
   * </ul>
   * 
   * @param refreshToken the JWT refresh token to be stored in the cookie
   * @return ResponseCookie configured with secure settings and the refresh token
   */
  public ResponseCookie createRefreshTokenCookie(String refreshToken) {
    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/api/v1/auth/refresh")
        .maxAge(refreshExpiration / 1000) // Convert seconds to milliseconds
        .sameSite("Strict")
        .build();

    return refreshCookie;
  }

  /**
   * Generates a JWT access token for the specified user.
   * 
   * <p>
   * The token includes default claims (uid, name, username) and is valid
   * for the duration specified by the jwt.expiration property.
   * 
   * @param user the user for whom to generate the token
   * @return JWT access token as a string
   */
  public String generateToken(User user) {
    return buildToken(new HashMap<>(), user, jwtExpiration);
  }

  /**
   * Generates a JWT access token with additional custom claims.
   * 
   * <p>
   * This method allows adding extra claims beyond the default user information.
   * 
   * @param extraClaims additional claims to include in the token
   * @param user        the user for whom to generate the token
   * @return JWT access token as a string
   */
  public String generateToken(Map<String, Object> extraClaims, User user) {
    return buildToken(extraClaims, user, jwtExpiration);
  }

  /**
   * Generates a JWT refresh token for the specified user.
   * 
   * <p>
   * Refresh tokens have a longer expiration time than access tokens and are used
   * to obtain new access tokens without requiring re-authentication.
   * 
   * @param user the user for whom to generate the refresh token
   * @return JWT refresh token as a string
   */
  public String generateRefreshToken(User user) {
    return buildToken(new HashMap<>(), user, refreshExpiration);
  }

  /**
   * Validates a JWT token and returns detailed validation results.
   * 
   * @param token the JWT token string to validate
   * @return JwtValidationResult containing validation status, expiration info,
   *         username, and status details
   *         - If valid: returns result with valid=true, expired=false, username
   *         extracted from token
   *         - If expired: returns result with valid=false, expired=true, username
   *         from expired claims
   *         - If invalid: returns result with valid=false, expired=false,
   *         username=null, and specific error status
   */
  public JwtValidationResult validateToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      String username = claims.getSubject();

      return JwtValidationResult.builder()
          .valid(true)
          .expired(false)
          .username(username)
          .status(JwtValidationResult.ValidationStatus.VALID)
          .build();

    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      return JwtValidationResult.builder()
          .valid(false)
          .expired(true)
          .username(e.getClaims().getSubject())
          .status(JwtValidationResult.ValidationStatus.EXPIRED)
          .build();

    } catch (io.jsonwebtoken.security.SignatureException e) {
      return JwtValidationResult.builder()
          .valid(false)
          .expired(false)
          .username(null)
          .status(JwtValidationResult.ValidationStatus.INVALID_SIGNATURE)
          .build();

    } catch (io.jsonwebtoken.MalformedJwtException e) {
      return JwtValidationResult.builder()
          .valid(false)
          .expired(false)
          .username(null)
          .status(JwtValidationResult.ValidationStatus.MALFORMED)
          .build();

    } catch (io.jsonwebtoken.UnsupportedJwtException e) {
      return JwtValidationResult.builder()
          .valid(false)
          .expired(false)
          .username(null)
          .status(JwtValidationResult.ValidationStatus.UNSUPPORTED)
          .build();
    } catch (IllegalArgumentException e) {
      return JwtValidationResult.builder()
          .valid(false)
          .expired(false)
          .username(null)
          .status(JwtValidationResult.ValidationStatus.ILLEGAL)
          .build();
    } catch (io.jsonwebtoken.security.SecurityException e) {
      return JwtValidationResult.builder()
          .valid(false)
          .expired(false)
          .username(null)
          .status(JwtValidationResult.ValidationStatus.INVALID)
          .build();
    } catch (io.jsonwebtoken.JwtException e) {
      return JwtValidationResult.builder()
          .valid(false)
          .expired(false)
          .username(null)
          .status(JwtValidationResult.ValidationStatus.INVALID)
          .build();
    }
  }

  private String buildToken(Map<String, Object> extraClaims, User user, long expiration) {
    Map<String, Object> claims = new HashMap<>(extraClaims);
    claims.put("uid", user.getUid());
    claims.put("name", user.getName());

    return Jwts
        .builder()
        .setClaims(claims)
        .setSubject(user.getUsername())
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  private Claims extractAllClaims(String token) throws io.jsonwebtoken.JwtException {
    return Jwts
        .parserBuilder()
        .setSigningKey(getSignInKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private Key getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}