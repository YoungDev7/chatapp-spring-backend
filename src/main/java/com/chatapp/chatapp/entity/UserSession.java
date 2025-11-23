package com.chatapp.chatapp.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user_session")
public class UserSession {
    
    @Id
    @Column(name = "user_uid")
    private String userUid;
    
    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;
    
    @Column(name = "websocket_session_id")
    private String websocketSessionId;
    
    @UpdateTimestamp
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    public UserSession(String userUid) {
        this.userUid = userUid;
        this.isOnline = false;
    }
    
    public UserSession(String userUid, Boolean isOnline, String websocketSessionId) {
        this.userUid = userUid;
        this.isOnline = isOnline;
        this.websocketSessionId = websocketSessionId;
    }
}
