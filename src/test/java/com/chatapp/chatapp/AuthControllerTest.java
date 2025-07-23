package com.chatapp.chatapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.chatapp.chatapp.entity.Token;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.IUserRepository;
import com.chatapp.chatapp.repository.TokenRepository;
import com.chatapp.chatapp.test_util.MockJwtService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IUserRepository userRepository;


    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String validAccessToken;
    private String validRefreshToken;
    private String ValidRequestBody;
    private String InvalidRequestBody;
    private String url;
    private MockJwtService mockJwtService;

    // all tests indirectly test JwtAuthenticationFilter

    @BeforeEach
    void setUp() {
        //restTemplate = new RestTemplate();
        mockJwtService = new MockJwtService();

        ValidRequestBody = """
            {
                "email": "mikehock@email.com",
                "password": "cunt"
            }
            """;

        InvalidRequestBody = """
        {
            "email": "mikehock@email.com",
            "password": "wrongpassword"
        }
        """;
        
    }
    
    @Test
    void shouldAuthenticateUserSuccessfully() {
        url = "http://localhost:" + port + "/api/v1/auth/authenticate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(ValidRequestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().contains("access_token");
        assert response.getHeaders().get("Set-Cookie") != null;
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() {
        String url = "http://localhost:" + port + "/api/v1/auth/authenticate";

        //it needs diffrent httpclient in order for the failed request to not retry that way it can be read
        TestRestTemplate restTemplate = new TestRestTemplate();
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(InvalidRequestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

        assert response.getStatusCode() == HttpStatus.UNAUTHORIZED;
        assert response.getBody().contains("invalid email or password");
    }

    //this is more of a JwtAuthenticationFilter test
    @ParameterizedTest
    @MethodSource("accessTokenTestCases")
    void shouldHandleDifferentTokenTypes(Function<User, String> tokenGenerator, HttpStatus expectedStatus, String expectedMessage) {
        String url = "http://localhost:" + port + "/api/v1/auth/validateToken";
        User user = userRepository.findUserByEmail("gabeitch@example.com").orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        String token = tokenGenerator.apply(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);

        assert response.getStatusCode() == expectedStatus;
        assert response.getBody().contains(expectedMessage);
    }

    static Stream<Object[]> accessTokenTestCases() {
        MockJwtService mockJwtService = new MockJwtService();
        return Stream.of(
            new Object[]{(Function<User, String>) mockJwtService::generateValidToken, HttpStatus.OK, "valid"},
            new Object[]{(Function<User, String>) mockJwtService::generateExpiredToken, HttpStatus.UNAUTHORIZED, "Invalid token EXPIRED"},
            new Object[]{(Function<User, String>) mockJwtService::generateInvalidSignatureToken, HttpStatus.UNAUTHORIZED, "Invalid token INVALID_SIGNATURE"},
            new Object[]{(Function<User, String>) mockJwtService::generateMalformedToken, HttpStatus.UNAUTHORIZED, "Invalid token MALFORMED"},
            new Object[]{(Function<User, String>) mockJwtService::generateUnsupportedToken, HttpStatus.UNAUTHORIZED, "Invalid token UNSUPPORTED"}
            //missing illegal token test, havent found a way to pass null in parameters, as long as validateToken method test passes in JwtServiceTest then there 
            //is nothing to worry about 
        );
    }

    //refresh test: 
    // 1) valid but expired access token and valid refresh
    // 2) invalid access token but valid refresh
    // 3) valid access token but expired refresh
    @ParameterizedTest
    @MethodSource("refreshTokenTestCases")
    void shouldDiffrentRefreshRequests(Function<User, String> accessTokenGenerator, Function<User, String> refreshTokenGenerator, HttpStatus expectedStatus, String expectedMessage){
        String url = "http://localhost:" + port + "/api/v1/auth/refresh";
        
        TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());

        String accessToken;
        String refreshToken;

        try {
            User user = userRepository.findUserByEmail("gabeitch@example.com").orElseThrow(() -> new UsernameNotFoundException("User not found"));

            accessToken = accessTokenGenerator.apply(user);
            refreshToken = refreshTokenGenerator.apply(user);

            tokenRepository.deleteByUser(user);

            var refreshTokenEntity = Token.builder()
                .user(user)
                .token(refreshToken)
                .expired(false)
                .revoked(false)
                .build();
            tokenRepository.save(refreshTokenEntity);
            
            transactionManager.commit(transaction);
            
        } catch (Exception e) {
            transactionManager.rollback(transaction);
            throw e;
        }

        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Cookie", "refreshToken=" + refreshToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

        assertEquals(expectedStatus, response.getStatusCode());
        assertTrue(response.getBody().contains(expectedMessage));
        
        // Clean up after test
        TransactionStatus cleanupTransaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            tokenRepository.deleteByToken(refreshToken);
            transactionManager.commit(cleanupTransaction);
        } catch (Exception e) {
            transactionManager.rollback(cleanupTransaction);
        }
    }

    static Stream<Object[]> refreshTokenTestCases() {
        MockJwtService mockJwtService = new MockJwtService();
        return Stream.of(
            // [accessTokenGenerator, refreshTokenGenerator, expectedStatus, expectedMessage]
            new Object[]{
                (Function<User, String>) mockJwtService::generateExpiredToken,
                (Function<User, String>) mockJwtService::generateValidToken,
                HttpStatus.OK, 
                "access_token"
            },
            new Object[]{
                (Function<User, String>) mockJwtService::generateInvalidSignatureToken,
                (Function<User, String>) mockJwtService::generateValidToken,
                HttpStatus.UNAUTHORIZED, 
                "Invalid token INVALID_SIGNATURE"
            },
            new Object[]{
                (Function<User, String>) mockJwtService::generateValidToken,
                (Function<User, String>) mockJwtService::generateExpiredToken,
                HttpStatus.UNAUTHORIZED, 
                "Invalid token EXPIRED"
            }
        );
    }
}
