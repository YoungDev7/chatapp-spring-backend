package com.chatapp.chatapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.TokenRepository;
import com.chatapp.chatapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class LogoutServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private Authentication authentication;

    private User mockUser;

    @BeforeEach
    void setUp(){
        mockUser = new User("testUser", "password123", "test@example.com");
        //TODO: unfinished, use authServiceTest as reference
    }
}
