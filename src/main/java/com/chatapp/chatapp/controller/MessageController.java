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
    public Message handleMessage(@Payload Message message, Principal principal) {
        
        if (principal != null) {
            Authentication auth = (Authentication) principal;

            if (auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                
                if (user.getUid().equals(message.getSenderUid())) {
                    messageService.postNewMessage(message);
                    return message; //this broadcasts the message back to all subscribers
                } else {
                    ApplicationLogger.warningLog("user-sender mismatch: " + user.getUid() + " != " + message.getSenderUid());
                }
            }
        } else {
            ApplicationLogger.errorLog("Principal is null in handleMessage sender: " + message.getSender());
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
