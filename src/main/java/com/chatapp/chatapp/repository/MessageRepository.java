package com.chatapp.chatapp.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.chatapp.chatapp.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @NonNull
    List<Message> findAll();
    
    @Query("SELECT m FROM Message m WHERE m.chatView.id = :chatViewId ORDER BY m.createdAt ASC")
    List<Message> findByChatViewId(@Param("chatViewId") String chatViewId);
}
