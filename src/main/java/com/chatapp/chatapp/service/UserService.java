package com.chatapp.chatapp.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.IUserRepository;
import com.chatapp.chatapp.util.ApplicationLogger;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //get users
    public List<User> getUsers(){
        return userRepository.findAll();
    }

    //new user
    public void postNewUser(User user){
        if (user.getUid() != null) {
            throw new IllegalArgumentException("New user should not have an ID set");
        }
        
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        ApplicationLogger.infoLog("Saving new user: " + user.getName());
        userRepository.save(user);
    }

    //delete user
    public void deleteUser(String uid){
        if(userRepository.existsById(uid)){
            userRepository.deleteById(uid);
        }else {
            throw new IllegalStateException("user not found in database, ID:" + uid);
        }
    }

    //update user
    @Transactional
    public void updateUser(String id, String name, String email, String password){
        //checking if user exsits otherwise we throw exeption
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalStateException("user id" + id + "doesnt exist"));

        //update name
        if(name != null && name.length() > 0 && !Objects.equals(user.getName(), name)){
            user.setName(name);
        }

        //update email, check if email is aleady taken before we update
        if(email != null && email.length() > 0 && !Objects.equals(user.getEmail(), email)){
            Optional<User> userOptional = userRepository.findUserByEmail(email);
            
            if(userOptional.isPresent()){
                throw new IllegalStateException("email taken");
            }

            user.setEmail(email);
        }

        //update password
        //todo
    }
    
}
