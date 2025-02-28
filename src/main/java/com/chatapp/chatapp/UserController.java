package com.chatapp.chatapp;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public List<User> AllUsers(){
        return userService.getUsers();
    }

    @PostMapping
    public void postUser(@RequestBody User user){
        userService.postNewUser(user);
    }

    @DeleteMapping(path = "{uid}")
    public void deleteUser(@PathVariable("uid") Long uid){
        userService.deleteUser(uid);
    }

    @PutMapping(path = "{id}")
    public void updateUser(@PathVariable("id") Long id, @RequestParam(required = false) String name, @RequestParam(required = false) String email, @RequestParam(required = false) String password){
        userService.updateUser(id, name, email, password);
    }
}
