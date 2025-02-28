package com.chatapp.chatapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import java.util.*;

@Repository
public interface IUserRepository extends JpaRepository<User, Long>{
    @NonNull
    List<User> findAll();

    @Query("SELECT u FROM User u WHERE u.name = :name")
    Optional<User> findUserByName(@Param("name") String name);
    
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findUserByEmail(@Param("email") String email);
}
