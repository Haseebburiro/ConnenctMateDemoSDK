package com.arittek.connectmate;

import com.chat.befiler.config.ConnectMateConfig;
import com.chat.befiler.config.ConnectMateInstance;

public class ConnectMateConfigClass {
    public void connectConfig(String channelId){
        ConnectMateConfig configuration = new ConnectMateConfig(channelId);
        ConnectMateInstance.createInstanceWith(configuration);
    }
}
