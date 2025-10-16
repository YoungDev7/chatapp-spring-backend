package com.chatapp.chatapp.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.chatapp.Dto.MessageRequest;
import com.chatapp.chatapp.Dto.MessageResponse;
import com.chatapp.chatapp.entity.Message;
import com.chatapp.chatapp.entity.User;
import com.chatapp.chatapp.service.MessageService;
import com.chatapp.chatapp.util.ApplicationLogger;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Handles incoming WebSocket messages from authenticated users.
     * Validates that the sender matches the authenticated user before processing.
     * 
     * @param messageRequest The message request containing text and sender UID
     * @param principal The authenticated user principal from the WebSocket session
     * @return Message object that gets broadcasted to all subscribers via /topic/messages,
     *         or null if authentication fails or user mismatch occurs
     */
    @MessageMapping("/chat") // where client broadcasts
    @SendTo("/topic/messages") // where server broadcasts
    public MessageResponse handleMessage(@Payload MessageRequest messageRequest, Principal principal) {
        
        if (principal == null) {
            ApplicationLogger.errorLog("Principal is null in handleMessage");
            return null;
        } 

        Authentication auth = (Authentication) principal;

        if (auth.getPrincipal() instanceof User) {
            User authenticatedUser = (User) auth.getPrincipal();
            Message message = new Message(messageRequest.getText(), authenticatedUser);
            messageService.postNewMessage(message);
            return new MessageResponse(message.getText(), message.getSender().getName()); // this broadcasts the message back to all subscribers
        }

        return null;
    }
    
    /**
     * Retrieves all messages from the system.
     * 
     * @return List of all Message objects in the system
     */
    @GetMapping
    public List<MessageResponse> allMessages(){
        List<MessageResponse> messageListResponse = new ArrayList<>();
        List<Message> messageListFetched = messageService.getMessages();

        for(Message message : messageListFetched){
            messageListResponse.add(new MessageResponse(message.getText(), message.getSender().getName()));
        }

        return messageListResponse;
        
    }

}
