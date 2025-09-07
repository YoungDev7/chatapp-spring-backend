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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.chatapp.chatapp.DTO.JwtValidationResult;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.service.JwtService;
import com.chatapp.chatapp.test_util.MockJwtService;

import io.jsonwebtoken.Claims;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Mock
    private Claims mockClaims;
    
    private String mockRefreshToken;
    private String mockJwtToken;
    private MockJwtService mockJwtService;
    private User mockUser;

    @Spy
    @InjectMocks
    private JwtService jwtService;

    
    @BeforeEach
    void setUp() {   
        mockJwtService = new MockJwtService();

        mockJwtToken = "mock.jwt.token";
        mockRefreshToken = "mock.refresh.token";

        // @Value annotations are not being read so we have to map it to a normal variable which is also reason why MockJwtService
        //doesnt use @Value annotations 
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", mockJwtService.refreshExpiration);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", mockJwtService.jwtExpiration);
        ReflectionTestUtils.setField(jwtService, "secretKey", mockJwtService.secretKey);

        mockUser = new User("testUser", "password123", "test@example.com");
        mockUser.setUid("1");
    }

    //extractAllClaims and getSigningKey methods are called in validateToken so they are tested indirectly

    @Test
    void shouldSuccessfullyCreateRefreshTokenCookie() {
        //execute
        ResponseCookie refreshCookie = jwtService.createRefreshTokenCookie(mockRefreshToken);
        
        //verify
        assertNotNull(refreshCookie);
        assertTrue(refreshCookie.isHttpOnly());
        assertTrue(refreshCookie.isSecure());
        assertEquals("/api/v1/auth/refresh", refreshCookie.getPath());
        assertEquals(604800000, refreshCookie.getMaxAge().toMillis());
        assertEquals("Strict", refreshCookie.getSameSite());
    }

    @Test
    void shouldSuccessfullyValidateToken(){
        //setup
        String validToken = mockJwtService.generateValidToken(mockUser);
        //execute
        var validationResult = jwtService.validateToken(validToken);
        
        System.out.println(validToken);

        //verify
        assertTrue(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals("test@example.com", validationResult.getUsername());
        assertEquals(JwtValidationResult.ValidationStatus.VALID, validationResult.getStatus());
    }    

    @Test
    void shouldReturnExpiredStatus() {
        //setup
        String expiredToken = mockJwtService.generateExpiredToken(mockUser);
    
        System.out.println(expiredToken);        

        //execute
        var validationResult = jwtService.validateToken(expiredToken);

        //verify
        assertFalse(validationResult.isValid());
        assertTrue(validationResult.isExpired());
        assertEquals("test@example.com", validationResult.getUsername());
        assertEquals(JwtValidationResult.ValidationStatus.EXPIRED, validationResult.getStatus());
    }

    @Test
    void shouldReturnInvalidSignatureStatus() {
        //setup
        String invalidSignatureToken = mockJwtService.generateInvalidSignatureToken(mockUser);
    
        System.out.println(invalidSignatureToken);

        //execute
        var validationResult = jwtService.validateToken(invalidSignatureToken);

        //verify
        assertFalse(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(JwtValidationResult.ValidationStatus.INVALID_SIGNATURE, validationResult.getStatus());
    }

    @Test
    void shouldReturnMalformedStatus() {
        //setup
        String malformedToken = mockJwtService.generateMalformedToken(mockUser);
    
        System.out.println(malformedToken);

        //execute
        var validationResult = jwtService.validateToken(malformedToken);
        
        //verify
        assertFalse(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(JwtValidationResult.ValidationStatus.MALFORMED, validationResult.getStatus());
    }

    @Test
    void shouldReturnUnsupportedStatus() {
        //setup
        String unsupportedToken = mockJwtService.generateUnsupportedToken(mockUser);
    
        System.out.println(unsupportedToken);

        //execute
        var validationResult = jwtService.validateToken(unsupportedToken);
        
        //verify
        assertFalse(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(JwtValidationResult.ValidationStatus.UNSUPPORTED, validationResult.getStatus());
    }

    @Test
    void shouldReturnIllegalStatus() {
        //setup - using null token to trigger illegal argument exception
        
        //execute
        var validationResult = jwtService.validateToken(null);
        
        //verify
        assertFalse(validationResult.isValid());
        assertFalse(validationResult.isExpired());
        assertEquals(JwtValidationResult.ValidationStatus.ILLEGAL, validationResult.getStatus());
    }

    @Test
    void shouldGenerateToken(){
        //execute
        String token = jwtService.generateToken(mockUser);
        
        //verify
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    void shouuldGenerateRefreshToken() {
        //execute
        String token = jwtService.generateRefreshToken(mockUser);
        
        //verify
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }
}
