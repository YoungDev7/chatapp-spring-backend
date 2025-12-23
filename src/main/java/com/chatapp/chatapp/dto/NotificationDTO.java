package com.chatapp.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private String chatViewId;
    private NotificationType notificationType;

    public enum NotificationType{
        ADDED_TO_CHATVIEW
    }
}
