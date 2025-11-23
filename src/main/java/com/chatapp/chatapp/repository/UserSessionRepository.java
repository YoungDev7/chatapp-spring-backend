package com.chatapp.chatapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chatapp.chatapp.entity.UserSession;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    Optional<UserSession> findByUserUid(String userUid);
    
    Optional<UserSession> findByWebsocketSessionId(String websocketSessionId);
}
