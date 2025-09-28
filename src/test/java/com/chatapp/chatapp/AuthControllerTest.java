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


    private String ValidRequestBody;
    private String InvalidRequestBody;
    private String url;
    private MockJwtService mockJwtService;
    private String userOneEmail;
    private String userTwoEmail;

    // all tests indirectly test JwtAuthenticationFilter

    @BeforeEach
    void setUp() {
        mockJwtService = new MockJwtService();
        userOneEmail = "test1@email.com";
        userTwoEmail = "test2@email.com";

        ValidRequestBody = """
            {
                "email": "test1@email.com",
                "password": "testuserone"
            }
            """;

        InvalidRequestBody = """
        {
            "email": "test1@email.com",
            "password": "wrongpassword"
        }
        """;
        
    }
    
    @Test
    void shouldAuthenticateUserSuccessfully() {
        //setup
        url = "http://localhost:" + port + "/api/v1/auth/authenticate";

        //TODO: refactor, this block in each method should go into setup method (code duplication)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(ValidRequestBody, headers);

        //execute
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);
        
        //verify
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().contains("access_token");
        assert response.getHeaders().get("Set-Cookie") != null;
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() {
        //setup
        String url = "http://localhost:" + port + "/api/v1/auth/authenticate";

        //it needs diffrent httpclient in order for the failed request to not retry that way it can be read
        TestRestTemplate restTemplate = new TestRestTemplate();
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(InvalidRequestBody, headers);

        //execute
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

        //verify
        assert response.getStatusCode() == HttpStatus.UNAUTHORIZED;
        assert response.getBody().contains("invalid email or password");
    }

    //this is more of a JwtAuthenticationFilter test
    @ParameterizedTest
    @MethodSource("accessTokenTestCases")
    void shouldHandleDifferentTokenTypes(Function<User, String> tokenGenerator, HttpStatus expectedStatus, String expectedMessage) {
        //setup
        String url = "http://localhost:" + port + "/api/v1/auth/validateToken";
        User user = userRepository.findUserByEmail(userOneEmail).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        String token = tokenGenerator.apply(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> request = new HttpEntity<>(headers);

        //execute 
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);
        
        //verify
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
        //setup
        String url = "http://localhost:" + port + "/api/v1/auth/refresh";
        
        TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());

        String accessToken;
        String refreshToken;

        //we are setting up users refresh token in the database for each transaction
        try {
            User user = userRepository.findUserByEmail(userOneEmail).orElseThrow(() -> new UsernameNotFoundException("User not found"));

            //generates tokens 
            accessToken = accessTokenGenerator.apply(user);
            refreshToken = refreshTokenGenerator.apply(user);

            //deleting all existing tokens before adding new one 
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

        //request setup
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Cookie", "refreshToken=" + refreshToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        //execution, sending request
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, request, String.class);

        //verify
        assertEquals(expectedStatus, response.getStatusCode());
        assertTrue(response.getBody().contains(expectedMessage));
        
        // tear down
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
