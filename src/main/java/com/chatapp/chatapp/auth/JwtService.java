package com.chatapp.chatapp.auth;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;


@Service
public class JwtService {

  @Value("${application.security.jwt.secret-key}")
  private String secretKey;
  @Value("${application.security.jwt.expiration}")
  private long jwtExpiration;
  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshExpiration;

  
  public String generateToken(UserDetails userDetails) {
    return buildToken(new HashMap<>(), userDetails, jwtExpiration);
  }
  
  public String generateToken(Map<String, Object> extraClaims,UserDetails userDetails) {
    return buildToken(extraClaims, userDetails, jwtExpiration);
  }
  
  public String generateRefreshToken(UserDetails userDetails) {
    return buildToken(new HashMap<>(), userDetails, refreshExpiration);
  }

  public JwtValidationResult validateToken(String token) {
    try {
        Claims claims = extractAllClaims(token);
        String username = claims.getSubject();
        boolean expired = claims.getExpiration().before(new Date());
        
        return JwtValidationResult.builder()
                .valid(!expired)
                .expired(expired)
                .username(username)
                .status(expired ? JwtValidationResult.ValidationStatus.EXPIRED : JwtValidationResult.ValidationStatus.VALID)
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
                .status(JwtValidationResult.ValidationStatus.INVALID_SIGNATURE)
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

  private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
    return Jwts
    .builder()
    .setClaims(extraClaims)
    .setSubject(userDetails.getUsername())
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