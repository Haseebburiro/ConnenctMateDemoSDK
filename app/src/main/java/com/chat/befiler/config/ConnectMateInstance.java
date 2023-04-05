package com.chat.befiler.config;

public class ConnectMateInstance {
    private static ConnectMateInstance connectMateInstance;
    private ConnectMateConfig connectMateConfig;

    private ConnectMateInstance(){}
    private ConnectMateInstance(ConnectMateConfig connectMateConfig) {
        this.connectMateConfig = connectMateConfig;
    }

    public static ConnectMateInstance getInstance() {
        if(connectMateInstance == null) {
            throw new RuntimeException("Need call createInstanceWith method first!!");
        }
        return connectMateInstance;
    }

    public static ConnectMateInstance createInstanceWith(ConnectMateConfig configuration) {
        if(connectMateInstance == null) {
            synchronized(ConnectMateInstance.class) {
                if (connectMateInstance == null) {
                    connectMateInstance = new ConnectMateInstance(configuration);
                }
            }
        }
        return connectMateInstance;
    }

    public ConnectMateConfig getConfiguration() {
        return connectMateConfig;
    }
}