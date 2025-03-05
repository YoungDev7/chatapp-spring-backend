package com.chatapp.chatapp.repository;
import org.springframework.stereotype.Repository;

import com.chatapp.chatapp.entity.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import java.util.List;

@Repository
public interface IMessageRepository extends JpaRepository<Message, Long> {
    @NonNull
    List<Message> findAll();
}
