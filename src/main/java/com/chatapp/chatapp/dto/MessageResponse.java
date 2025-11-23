package com.chatapp.chatapp.dto;

import java.io.Serializable;
import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String text;
    private String senderName;
    private String senderUid;
    private String chatViewId;
    private ZonedDateTime createdAt;
}
