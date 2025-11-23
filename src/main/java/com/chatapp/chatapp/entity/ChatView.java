package com.chatapp.chatapp.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@Table(name = "chatview")
public class ChatView {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "chatview_users",
        joinColumns = @JoinColumn(name = "chatview_id"),
        inverseJoinColumns = @JoinColumn(name = "user_uid")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> users = new HashSet<>();
    
    @OneToMany(mappedBy = "chatView", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Message> messages = new HashSet<>();
    
    public ChatView(String name) {
        this.name = name;
    }

    public ChatView(String name, String id) {
        this.id = id;
        this.name = name;
    }
    
    public void addUser(User user) {
        this.users.add(user);
    }
    
    public void removeUser(User user) {
        this.users.remove(user);
    }
}
