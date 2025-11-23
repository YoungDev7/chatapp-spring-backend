package com.chatapp.chatapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.chatapp.chatapp.entity.ChatView;

@Repository
public interface ChatViewRepository extends JpaRepository<ChatView, String> {
    
    @Query("SELECT cv FROM ChatView cv JOIN cv.users u WHERE u.uid = :userUid")
    List<ChatView> findChatViewsByUserUid(@Param("userUid") String userUid);
    
    Optional<ChatView> findById(String id);
    
    @Query("SELECT cv FROM ChatView cv LEFT JOIN FETCH cv.users WHERE cv.id = :id")
    Optional<ChatView> findByIdWithUsers(@Param("id") String id);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO chatview (id, name) VALUES (:id, :name)", nativeQuery = true)
    void insertChatViewWithCustomId(@Param("id") String id, @Param("name") String name);
}
