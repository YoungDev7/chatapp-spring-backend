package com.chatapp.chatapp;

import java.security.Key;
import java.security.KeyPair;
import java.util.Date;
import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public final class TestJwtTokenService {
    protected String secretKey = "mockSecretKeymockSecretKeymockSecretKeymockSecretKeymockSecretKey";
    protected String fakeSecretKey = "fakeSecretKeyfakeSecretKeyfakeSecretKeyfakeSecretKeyfakeSecretKey";
    protected long jwtExpiration = 3600000;
    protected long refreshExpiration = 604800000; 

    protected String generateValidToken(UserDetails userDetails) {
        return buildToken(Map.of(), userDetails, jwtExpiration);
    }

    protected String generateExpiredToken(UserDetails userDetails) {
        return buildToken(Map.of(), userDetails, 0);
    }

    protected String generateInvalidSignatureToken(UserDetails userDetails) {
        return Jwts
        .builder()
        .setClaims(Map.of())
        .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getFakeSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    protected String generateMalformedToken(UserDetails userDetails) {
        String token = buildToken(Map.of(), userDetails, jwtExpiration);
        char[] tokenChars = token.toCharArray();
        tokenChars[20] = 'X';  
        return new String(tokenChars);
    }

    protected String generateUnsupportedToken(UserDetails userDetails) {
        try {
            // Generate RSA key pair
            KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
            
            return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256) // RSA algorithm
                .compact();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate unsupported token", e);
        }
    }
        

    protected String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
        .builder()
        .setClaims(extraClaims)
        .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    protected Claims extractAllClaims(String token) throws io.jsonwebtoken.JwtException {
        return Jwts
            .parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    protected Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    protected Key getFakeSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(fakeSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
