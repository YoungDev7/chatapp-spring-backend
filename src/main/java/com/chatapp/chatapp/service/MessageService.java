package com.chatapp.chatapp.service;

import java.util.List;
import java.util.Optional;

//import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.chatapp.chatapp.entity.Message;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    //get messages
    public List<Message> getMessages(){
        return messageRepository.findAll();
    }

    /**
     * Posts a new message to the database after validating that the sender exists.
     * 
     * This method verifies that the message sender is a valid user in the database
     * before saving the message. WebSocket broadcasting is handled separately in the controller.
     * 
     * @param message the message to be saved, must contain a valid sender with UID
     * @throws IllegalStateException if the sender user is not found in the database
     */
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
