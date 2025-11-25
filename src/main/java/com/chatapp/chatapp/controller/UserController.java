package com.chatapp.chatapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.chatapp.dto.UserResponse;
import com.chatapp.chatapp.service.UserService;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService; 

    @GetMapping("/avatar")
    public ResponseEntity<?> avatar(){
        String avatarLink = userService.getUserAvatar();
        log.info("retrieved users avatar");
        return ResponseEntity.ok(avatarLink);
    }

    @PatchMapping("/avatar")
    public ResponseEntity<?> avatar(@RequestBody String newAvatarLink) {
        try{
            userService.updateUserAvatar(newAvatarLink);
            log.info("updated users avatar");
            return ResponseEntity.ok("avatar updated");
        }catch(IllegalArgumentException e){
            log.info("error updating users avatar: {}", e);
            return ResponseEntity.status(400).body("string is null");
        }        
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUser(@RequestBody String searchQuery){
        try{
            UserResponse searchResult = userService.searchUser(searchQuery);
            log.info("returned successful search result: {}; search query: {}", searchResult.getUid(), searchQuery);
            return ResponseEntity.ok().body(searchResult);
        }catch(IllegalArgumentException e){
            log.info("unsuccessful user search: {}", e);
            return ResponseEntity.status(400).body(e);
        }catch(UsernameNotFoundException e){
            log.info("unsuccessful user search: {}", e);
            return ResponseEntity.status(400).body(e);
        }
    }
}
