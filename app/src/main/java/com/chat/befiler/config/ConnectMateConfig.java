package com.chat.befiler.config;

public class ConnectMateConfig {
    private String channelId;

    public ConnectMateConfig(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
