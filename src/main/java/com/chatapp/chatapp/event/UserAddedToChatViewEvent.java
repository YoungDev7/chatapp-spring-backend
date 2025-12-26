package com.chatapp.chatapp.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserAddedToChatViewEvent {
    private final String userUid;
    private final String chatViewId;
}
