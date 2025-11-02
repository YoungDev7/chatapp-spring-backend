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
 * ATTENTION
 * when refactoring or updating this class make sure that changes are reflexted 
 * in MockJwtService.java, both files need to be in sync for accurate testing pourposes
 */ 
@Service
public class JwtService {

  @Value("${application.security.jwt.secret-key}")
  private String secretKey;
  @Value("${application.security.jwt.expiration}")
  private long jwtExpiration;
  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshExpiration;

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

  public String generateToken(User user) {
    return buildToken(new HashMap<>(), user, jwtExpiration);
  }
  
  public String generateToken(Map<String, Object> extraClaims, User user) {
    return buildToken(extraClaims, user, jwtExpiration);
  }
  
  public String generateRefreshToken(User user) {
    return buildToken(new HashMap<>(), user, refreshExpiration);
  }

  /**
   * Validates a JWT token and returns detailed validation results.
   * 
   * @param token the JWT token string to validate
   * @return JwtValidationResult containing validation status, expiration info, username, and status details
   *         - If valid: returns result with valid=true, expired=false, username extracted from token
   *         - If expired: returns result with valid=false, expired=true, username from expired claims
   *         - If invalid: returns result with valid=false, expired=false, username=null, and specific error status
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
    }catch (IllegalArgumentException e) {
        return JwtValidationResult.builder()
                .valid(false)
                .expired(false)
                .username(null)
                .status(JwtValidationResult.ValidationStatus.ILLEGAL)
                .build();
    }catch(io.jsonwebtoken.security.SecurityException e){
        return JwtValidationResult.builder()
                .valid(false)
                .expired(false)
                .username(null)
                .status(JwtValidationResult.ValidationStatus.INVALID)
                .build();
    }catch(io.jsonwebtoken.JwtException e) {
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