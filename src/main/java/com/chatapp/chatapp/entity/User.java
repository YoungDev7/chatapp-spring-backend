package com.chatapp.chatapp.entity;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid")
    private String uid;

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "password")
    private String password;
    
    @Column(name = "email", unique = true)
    private String email;

    @Column(name ="avatar_link", nullable = true)
    private String avatarLink;

    public User(String name, String password, String email) {
        this.name = name;
        this.password = password;
        this.email = email;
    }

    public User(String name, String password, String email, String avatarLink) {
        this.name = name;
        this.password = password;
        this.email = email;
        this.avatarLink = avatarLink;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();    
    }

    @Override
    public String getPassword() {
      return password;
    }
  
    @Override
    public String getUsername() {
      return email; //THIS SUCKS TODO: CHANGE THIS
    }
  
    @Override
    public boolean isAccountNonExpired() {
      return true;
    }
  
    @Override
    public boolean isAccountNonLocked() {
      return true;
    }
  
    @Override
    public boolean isCredentialsNonExpired() {
      return true;
    }
  
    @Override
    public boolean isEnabled() {
      return true;
    }   
}
