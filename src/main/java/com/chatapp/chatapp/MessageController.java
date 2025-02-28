package com.chatapp.chatapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;


import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;
    

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    //websocket endpoint
    @MessageMapping("/chat") //where client broadcasts
    @SendTo("/topic/messages") //where server broadcasts
    public Message handleMessage(@Payload Message message) {
        messageService.postNewMessage(message);
        //websocket sends it back to the client
        return message;
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
