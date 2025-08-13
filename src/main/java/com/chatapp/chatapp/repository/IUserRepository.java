package com.chatapp.chatapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.chatapp.chatapp.entity.User;

@Repository
public interface IUserRepository extends JpaRepository<User, String>{
    @NonNull
    List<User> findAll();

    @Query("SELECT u FROM User u WHERE u.name = :name")
    Optional<User> findUserByName(@Param("name") String name);
    
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findUserByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.uid = :uid")
    Optional<User> findUserByUid(@Param("uid") String uid);
}
