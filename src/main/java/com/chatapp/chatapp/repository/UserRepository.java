package com.chatapp.chatapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.chatapp.chatapp.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String>{
    @NonNull
    List<User> findAll();

    Optional<User> findUserByNameIgnoreCase(String username);

    Optional<User> findUserByName(@Param("name") String name);
    
    Optional<User> findUserByEmail(@Param("email") String email);
    
    Optional<User> findUserByUid(@Param("uid") String uid);
}
