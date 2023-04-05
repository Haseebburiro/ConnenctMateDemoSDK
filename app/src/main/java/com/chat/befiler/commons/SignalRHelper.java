package com.chat.befiler.commons;

import static com.chat.befiler.commons.Constants.ChatHubUrl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.chat.befiler.activities.LoginActivity;
import com.chat.befiler.fragments.ReLoadConversationEvent;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

import org.greenrobot.eventbus.EventBus;

import io.reactivex.Single;

public class SignalRHelper {


    public HubConnection createChatHubConnection(String accessToken, Context context){
        HubConnection hubConnection =
                HubConnectionBuilder.create(ChatHubUrl).withAccessTokenProvider(Single.defer(() -> {
                    // Your logic here.
                    return Single.just(accessToken);
                })).build();
        return hubConnection;
    }

    @SuppressLint("CheckResult")
    public boolean startSignalRHubClient(HubConnection hubConnection , int agentId, Context context, Activity activity){
        if (hubConnection!=null){
            final boolean[] isConnected = new boolean[1];
            try {
                hubConnection.start().doOnComplete(() -> {
                    isConnected[0] = true;
                }).blockingAwait();
                hubConnection.setKeepAliveInterval(2000);
//                hubConnection.invoke("AgentJoined",agentId);
                hubConnection.invoke("CutomerJoinedFromWidget","6901b42a-0776-41d2-ac76-6cb6f3029d53");
                new Common().saveConnectionId(context,hubConnection.getConnectionId());
                if (isConnected[0]){
                    return true;
                }else{
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (e.getMessage().equalsIgnoreCase("Unexpected status code returned from negotiate: 401 Unauthorized.")){
                    //logout callhere
                    Common common = new Common();
                    common.savePermission(context,"");
                    common.saveIsSuperAdmin(context,"");
                    common.isLoggedIn(context,false);
                    common.saveSelectedMenu(context,"0");
                    Intent intent = new Intent(context, LoginActivity.class);
                    intent.putExtra(Constants.CONVERSATION_BY_UID_KEY,"");
                    context.startActivity(intent);
                    activity.finish();
                }else{
                    EventBus.getDefault().post(new ReLoadConversationEvent("Reconnecting"));
                }
            }
        }
        return false;
    }

}
