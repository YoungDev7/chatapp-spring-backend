package com.chatapp.chatapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.ok(avatarLink);
    }

    @PatchMapping("/avatar")
    public ResponseEntity<?> avatar(@RequestBody String newAvatarLink) {
        try{
            userService.updateUserAvatar(newAvatarLink);
            return ResponseEntity.ok("avatar updated");
        }catch(IllegalArgumentException e){
            return ResponseEntity.status(400).body("string is null");
        }        
    }
    
}
