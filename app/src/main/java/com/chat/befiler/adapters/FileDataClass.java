package com.chat.befiler.adapters;

public class FileDataClass {
    public String fileName;
    public String fileSizes;
    public String mimeType;
    public String url;
    public String tempChatId;

    public FileDataClass(String fileName, String fileSizes,String mimeType,String url,String tempChatId) {
        this.fileName = fileName;
        this.fileSizes = fileSizes;
        this.mimeType = mimeType;
        this.url = url;
        this.tempChatId = tempChatId;
    }
}
