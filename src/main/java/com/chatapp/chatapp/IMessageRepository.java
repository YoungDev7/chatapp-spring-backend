package com.chatapp.chatapp;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import java.util.List;

@Repository
public interface IMessageRepository extends JpaRepository<Message, Long> {
    @NonNull
    List<Message> findAll();
}
