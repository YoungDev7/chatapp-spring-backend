package com.chatapp.chatapp.test_util;

import java.security.Key;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;

import com.chatapp.chatapp.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

//this service is needed to due to some methods in JwtService being private
//also it helps to have methods that generate specific tokens for testing purposes

@Profile("test")
public final class MockJwtService {
    //cannot read from @Value annotations in tests so we have to hardcode the values
    //values have to correspond to  application-test.properties !!!
    public String secretKey = "mockSecretKeymockSecretKeymockSecretKeymockSecretKeymockSecretKey";
    public String fakeSecretKey = "fakeSecretKeyfakeSecretKeyfakeSecretKeyfakeSecretKeyfakeSecretKey";
    public long jwtExpiration = 3600000;
    public long refreshExpiration = 604800000; 

    public String generateValidToken(User user) {
        return buildToken(Map.of(), user, jwtExpiration);
    }

    public String generateExpiredToken(User user) {
        return buildToken(Map.of(), user, 0);
    }

    public String generateInvalidSignatureToken(User user) {
        return Jwts
        .builder()
        .setClaims(getDefaultClaims(user))
        .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getFakeSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateMalformedToken(User user) {
        String token = buildToken(Map.of(), user, jwtExpiration);
        char[] tokenChars = token.toCharArray();
        tokenChars[20] = 'X';  
        return new String(tokenChars);
    }

    public String generateUnsupportedToken(User user) {
        try {
            // Generate RSA key pair
            KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
            
            return Jwts.builder()
                .setClaims(getDefaultClaims(user))
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256) // RSA algorithm
                .compact();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate unsupported token", e);
        }
    }

        

    public String buildToken(Map<String, Object> extraClaims, User user, long expiration) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("uid", user.getUid().toString());
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

    public Claims extractAllClaims(String token) throws io.jsonwebtoken.JwtException {
        return Jwts
            .parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Key getFakeSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(fakeSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    private Map<String, Object> getDefaultClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getUid().toString());
        claims.put("name", user.getName());
        return claims;
    }
}
