package com.chatapp.chatapp.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.chatapp.DTO.MessageRequest;
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

    // Secured WebSocket endpoint
    @MessageMapping("/chat") // where client broadcasts
    @SendTo("/topic/messages") // where server broadcasts
    public Message handleMessage(@Payload MessageRequest messageRequest, Principal principal) {
        
        if (principal != null) {
            Authentication auth = (Authentication) principal;

            if (auth.getPrincipal() instanceof User) {
                User authenticatedUser = (User) auth.getPrincipal();
                
                if (authenticatedUser.getUid().equals(messageRequest.getSenderUid())) {
                    Message message = new Message(messageRequest.getText(), authenticatedUser);
                    messageService.postNewMessage(message);
                    return message; //this broadcasts the message back to all subscribers
                } else {
                    ApplicationLogger.errorLog("user-sender mismatch: " + authenticatedUser.getUid() + " != " + messageRequest.getSenderUid());
                }
            }
        } else {
            ApplicationLogger.errorLog("Principal is null in handleMessage sender: " + messageRequest.getSenderUid());
        }

        return null;
    }
    
    @GetMapping
    public List<Message> AllMessages(){
        return messageService.getMessages();
    }

    @PostMapping
    public void postMessage(@RequestBody Message message){
        messageService.postNewMessage(message);
    }

    @DeleteMapping(path = "{id}")
    public void deleteMessage(@PathVariable("id") Long id){
        messageService.deleteMessage(id);
    }

}
