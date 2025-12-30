package com.chatapp.chatapp.entity;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "text")
    private String text;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_uid")
    @JsonIgnoreProperties({ "password", "authorities", "accountNonExpired",
            "accountNonLocked", "credentialsNonExpired", "enabled", "username" })
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatview_id", nullable = false)
    @JsonIgnoreProperties({ "users", "messages" })
    private ChatView chatView;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public Message(String text, User sender) {
        this.text = text;
        this.sender = sender;
    }

    public Message(String text, User sender, ChatView chatView, ZonedDateTime createdAt) {
        this.text = text;
        this.sender = sender;
        this.chatView = chatView;
        this.createdAt = createdAt;
    }
}
