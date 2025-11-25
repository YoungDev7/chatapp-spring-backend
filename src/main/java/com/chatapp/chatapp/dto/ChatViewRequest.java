package com.chatapp.chatapp.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatViewRequest {
    private String name;
    private Set<String> userUids;
}
