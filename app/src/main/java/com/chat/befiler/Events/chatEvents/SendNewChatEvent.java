package com.chat.befiler.Events.chatEvents;

import com.chat.befiler.model.chat.NewChatModel;
import com.chat.befiler.model.chat.SendMessageModel;

public class SendNewChatEvent {
    public NewChatModel sendMessageModel;
    public String eventType;

    public SendNewChatEvent(NewChatModel sendMessageModel, String eventType) {
        this.sendMessageModel = sendMessageModel;
        this.eventType = eventType;
    }
}
