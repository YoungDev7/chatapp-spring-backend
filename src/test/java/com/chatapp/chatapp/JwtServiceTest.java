package com.chatapp.chatapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import com.chatapp.chatapp.auth.JwtValidationResult;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.service.JwtService;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Mock
    private Claims mockClaims;
    
    private String mockRefreshToken;
    private String mockJwtToken;
    private TestJwtTokenService testJwtTokenProvider;
    private User mockUser;

    @Spy
    @InjectMocks
    private JwtService jwtService;
    
    @BeforeEach
    void setUp() {   
        testJwtTokenProvider = new TestJwtTokenService();

        mockJwtToken = "mock.jwt.token";
        mockRefreshToken = "mock.refresh.token";

        ReflectionTestUtils.setField(jwtService, "refreshExpiration", testJwtTokenProvider.refreshExpiration);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", testJwtTokenProvider.jwtExpiration);
        ReflectionTestUtils.setField(jwtService, "secretKey", testJwtTokenProvider.secretKey);

        mockUser = new User("testUser", "password123", "test@example.com");
    }

    //extractAllClaims and getSigningKey methods are called in validateToken so they are tested indirectly

    @Test
    void shouldSuccessfullyCreateRefreshTokenCookie() {
        ResponseCookie refreshCookie = jwtService.createRefreshTokenCookie(mockRefreshToken);
        
        assertNotNull(refreshCookie);
        assertTrue(refreshCookie.isHttpOnly());
        assertTrue(refreshCookie.isSecure());
        assertEquals("/api/v1/auth/refresh", refreshCookie.getPath());
        assertEquals(604800000, refreshCookie.getMaxAge().toMillis());
        assertEquals("Strict", refreshCookie.getSameSite());
    }

    @Test
    void shouldSuccessfullyValidateToken(){
        String validToken = testJwtTokenProvider.generateValidToken(mockUser);
        var validationResult = jwtService.validateToken(validToken);
        
        System.out.println(validToken);

        assertTrue(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals("test@example.com", validationResult.getUsername());
        assertEquals(JwtValidationResult.ValidationStatus.VALID, validationResult.getStatus());
    }    

    @Test
    void shouldReturnExpiredStatus() {
        String expiredToken = testJwtTokenProvider.generateExpiredToken(mockUser);
    
        System.out.println(expiredToken);        

        var validationResult = jwtService.validateToken(expiredToken);
        assertFalse(validationResult.isValid());
        assertTrue(validationResult.isExpired());
        assertEquals("test@example.com", validationResult.getUsername());
        assertEquals(JwtValidationResult.ValidationStatus.EXPIRED, validationResult.getStatus());
    }

    @Test
    void shouldReturnInvalidSignatureStatus() {
        String invalidSignatureToken = testJwtTokenProvider.generateInvalidSignatureToken(mockUser);
    
        System.out.println(invalidSignatureToken);

        var validationResult = jwtService.validateToken(invalidSignatureToken);
        assertFalse(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(JwtValidationResult.ValidationStatus.INVALID_SIGNATURE, validationResult.getStatus());
    }

    @Test
    void shouldReturnMalformedStatus() {
        String malformedToken = testJwtTokenProvider.generateMalformedToken(mockUser);
    
        System.out.println(malformedToken);

        var validationResult = jwtService.validateToken(malformedToken);
        assertFalse(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(JwtValidationResult.ValidationStatus.MALFORMED, validationResult.getStatus());
    }

    @Test
    void shouldReturnUnsupportedStatus() {
        String unsupportedToken = testJwtTokenProvider.generateUnsupportedToken(mockUser);
    
        System.out.println(unsupportedToken);

        var validationResult = jwtService.validateToken(unsupportedToken);
        assertFalse(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(JwtValidationResult.ValidationStatus.UNSUPPORTED, validationResult.getStatus());
    }

    @Test
    void shouldReturnIllegalStatus() {
        var validationResult = jwtService.validateToken(null);
        assertFalse(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(JwtValidationResult.ValidationStatus.ILLEGAL, validationResult.getStatus());
    }

    @Test
    void shouldGenerateToken(){
        String token = jwtService.generateToken(mockUser);
        
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    void shouuldGenerateRefreshToken() {
        String token = jwtService.generateRefreshToken(mockUser);
        
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }
}
