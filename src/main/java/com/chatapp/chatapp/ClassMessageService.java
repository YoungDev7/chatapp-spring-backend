package com.chatapp.chatapp;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


@Service
public class ClassMessageService {
    
    @Autowired
    //private SimpMessagingTemplate messagingTemplate;
    private final IMessageRepository messageRepository;
    private final IUserRepository userRepository;

    @Autowired
    public ClassMessageService(IMessageRepository messageRepository, IUserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    //get messages
    public List<ClassMessage> getMessages(){
        return messageRepository.findAll();
    }

    //post new message to database after checking if user is valid 
    public void postNewMessage(ClassMessage message){
        Optional<ClassUser> userOptional = userRepository.findUserByName(message.getSender());

        if(userOptional.isPresent()){
            messageRepository.save(message);
            //websocket
            //messagingTemplate.convertAndSend("/topic/messages", message);
        }else {
            throw new IllegalStateException("user not found in database, name: " + message.getSender());
        }
        //System.out.println(message);
    }

    //delete message
    public void deleteMessage(Long id){
        if(messageRepository.existsById(id)){
            messageRepository.deleteById(id);
        }else {
            throw new IllegalStateException("message not found in database, ID:" + id);
        }
    }


}
