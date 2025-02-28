package com.chatapp.chatapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import java.util.*;

@Repository
public interface IUserRepository extends JpaRepository<ClassUser, Long>{
    @NonNull
    List<ClassUser> findAll();

    @Query("SELECT u FROM ClassUser u WHERE u.name = :name")
    Optional<ClassUser> findUserByName(@Param("name") String name);
    
    @Query("SELECT u FROM ClassUser u WHERE u.email = :email")
    Optional<ClassUser> findUserByEmail(@Param("email") String email);
}
