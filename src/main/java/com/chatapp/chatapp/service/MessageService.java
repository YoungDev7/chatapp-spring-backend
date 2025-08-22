package com.chatapp.chatapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.entity.Message;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.IMessageRepository;
import com.chatapp.chatapp.repository.IUserRepository;


@Service
public class MessageService {
    
    @Autowired
    //private SimpMessagingTemplate messagingTemplate;
    private final IMessageRepository messageRepository;
    private final IUserRepository userRepository;

    @Autowired
    public MessageService(IMessageRepository messageRepository, IUserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    //get messages
    public List<Message> getMessages(){
        return messageRepository.findAll();
    }

    //post new message to database after checking if user is valid 
    public void postNewMessage(Message message){
        Optional<User> userOptional = userRepository.findUserByUid(message.getSender().getUid());

        if(userOptional.isPresent()){
            messageRepository.save(message);
            //websocket (handled in controller)
            //messagingTemplate.convertAndSend("/topic/messages", message);
        }else {
            throw new IllegalStateException("user not found in database, name: " + message.getSender().getUid());
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
