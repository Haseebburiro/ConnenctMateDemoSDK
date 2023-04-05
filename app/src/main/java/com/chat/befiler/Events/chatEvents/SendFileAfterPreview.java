package com.chat.befiler.Events.chatEvents;

public class SendFileAfterPreview {

    public String fileMessage;
    public String fileUri;
    public String fileType;
    public String fileName;
    public String eventType;


    public SendFileAfterPreview(String fileMessage, String fileUri, String fileType, String fileName, String eventType) {
        this.fileMessage = fileMessage;
        this.fileUri = fileUri;
        this.fileType = fileType;
        this.fileName = fileName;
        this.eventType = eventType;
    }
}
