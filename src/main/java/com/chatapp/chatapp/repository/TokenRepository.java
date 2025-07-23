package com.chatapp.chatapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.chatapp.chatapp.entity.Token;
import com.chatapp.chatapp.entity.User;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    
    @Query(value = """
      select t from Token t inner join User u\s
      on t.user.id = u.id\s
      where u.id = :id and (t.expired = false and t.revoked = false)\s
      """)
    List<Token> findAllValidTokenByUser(Long id);

    Optional<Token> findByToken(String token);

    void deleteByUser(User user);

    void deleteByToken(String token);
  
}
