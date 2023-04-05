package com.chat.befiler.activities;

import static com.chat.befiler.commons.Constants.COLOR_CODE_KEY;
import static com.chat.befiler.commons.Constants.NAME_KEY;
import static com.chat.befiler.commons.Constants.NAME_LETTER_KEY;
import static com.chat.befiler.commons.Constants.SOURCE_KEY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.chat.befiler.Events.appEvents.MessageEvent;
import com.chat.befiler.Events.chatEvents.ReceiveMessageEvent;
import com.chat.befiler.Events.chatEvents.SendChatEvent;
import com.chat.befiler.Events.chatEvents.SendFileAfterPreview;
import com.chat.befiler.Events.chatEvents.SendNewChatEvent;
import com.chat.befiler.baseClasses.BaseActivity;
import com.chat.befiler.commons.ConnectionService;
import com.chat.befiler.commons.Constants;
import com.chat.befiler.commons.PermissionHelper;
import com.chat.befiler.commons.SignalRHelper;
import com.chat.befiler.commons.Common;
import com.chat.befiler.config.ConnectMateConfig;
import com.chat.befiler.config.ConnectMateInstance;
import com.chat.befiler.fragments.ConversationsDetailFragment;
import com.chat.befiler.fragments.ReLoadConversationEvent;
import com.chat.befiler.model.chat.Conversation;
import com.chat.befiler.model.chat.NewChatRecieveResponse;
import com.chat.befiler.model.chat.OnErrorData;
import com.chat.befiler.model.chat.RecieveMessage;
import com.chat.befiler.retrofit.ApiClient;
import com.chat.befiler.retrofit.WebResponse;
import com.example.signalrtestandroid.R;
import com.google.android.material.navigation.NavigationView;
import com.microsoft.signalr.HubConnection;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConnectMateMainActivity extends BaseActivity implements ConnectionService.ConnectionServiceCallback {

    Timer timer = null;
    Common common;
    public boolean isAlreadyConnected = true;
    public boolean isSignalRConnected;
    public boolean isReconnecting = true ;
    SignalRHelper signalRHelper;
    HubConnection hubConnection;
    int agentId;
    String isSuperAdmin = "";
    String selectMenu = "0";
    RelativeLayout menuClick,action_bar,rlUserProfile,layoutAllAssignCountMain,layoutAssignToMeCountMain,layoutResolvedCountMain,layoutNewCountMain ;

    private DrawerLayout mDrawer;
    private Context mContext;
    private LinearLayout LayoutReconnecting;
    private ImageView icSource;
    private TextView txtStatus;

    // Make sure to be using androidx.appcompat.app.ActionBarDrawerToggle version.
    private ActionBarDrawerToggle drawerToggle;
    private String notificationId = "";
    Intent intent1 = null;
    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setTurnScreenOn(true);
        } else {
            final Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        mContext = ConnectMateMainActivity.this;
        intent1 = getIntent();
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        action_bar = findViewById(R.id.action_bar);
        menuClick = findViewById(R.id.menuClick);
        LayoutReconnecting = findViewById(R.id.LayoutReconnecting);
        icSource = findViewById(R.id.icSource);
        txtStatus = findViewById(R.id.txtStatus);
        Glide.with(mContext).load(R.drawable.connecting).into(icSource);
        common = new Common();
        signalRHelper = new SignalRHelper();

        // assume imvLogo is an existing ImageView
        ConnectMateConfig connectMateConfig = ConnectMateInstance.getInstance().getConfiguration();
        if(connectMateConfig!=null){
            if(!connectMateConfig.getChannelId().isEmpty()){
                getAccessTokenByChannelId(connectMateConfig.getChannelId());
            }
        }
        if (!common.getUserId(this).isEmpty()) {
            agentId = Integer.parseInt(common.getUserId(this));
        }
        if (!common.getIsSuperAdmin(this).isEmpty()) {
            isSuperAdmin = common.getIsSuperAdmin(this);
        }
        if (!common.getIsSuperAdmin(this).isEmpty()) {
            if(intent1.getStringExtra(Constants.CONVERSATION_BY_UID_KEY)!=null && intent1.getExtras().containsKey(Constants.CONVERSATION_BY_UID_KEY)){
                notificationId = intent1.getStringExtra(Constants.CONVERSATION_BY_UID_KEY);
            }
        }


        // Find our drawer view
        mDrawer = findViewById(R.id.drawer_layout);
        menuClick.setOnClickListener(view -> {
            if(mDrawer.isDrawerOpen(GravityCompat.START)) {
                mDrawer.openDrawer(GravityCompat.END);
            }else{
                mDrawer.openDrawer(GravityCompat.START);
            }

        });

         NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
         View hView = navigationView.getHeaderView(0);
         startConnectionCheckService();
         if (Build.VERSION.SDK_INT >= 32) {
            notificationPermission();
         }
         ReplaceFragment(new ConversationsDetailFragment(), false, new Bundle(), true);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
                notificationId = intent.getStringExtra(Constants.CONVERSATION_BY_UID_KEY);
                //switch to notification screen when notificationCategoryId is empty
            if (notificationId != null && !notificationId.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putString(NAME_KEY, common.getConversationData(mContext).customerName);
                bundle.putString(NAME_LETTER_KEY, common.firstCharactorCapital(common.getConversationData(mContext).customerName));
                bundle.putString(SOURCE_KEY, common.getConversationData(mContext).source);
                bundle.putString(COLOR_CODE_KEY, "000000");
                bundle.putString(Constants.CONVERSATION_BY_UID_KEY, notificationId);
                ReplaceFragmentWithoutClearBackStack(new ConversationsDetailFragment(), true, bundle, true);
            }
        }
    }

    private void notificationPermission() {

        PermissionHelper.grantPermission(this, Manifest.permission.POST_NOTIFICATIONS, new PermissionHelper.PermissionInterface() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError() {
                ReplaceFragment(new ConversationsDetailFragment(), false, new Bundle(), true);
            }
        });
    }

    public void startConnectionCheckService(){
        Intent intent = new Intent(this, ConnectionService.class);
        // Interval in seconds
        intent.putExtra(ConnectionService.TAG_INTERVAL, 3);
        // URL to ping
        intent.putExtra(ConnectionService.TAG_URL_PING, "http://www.google.com");
        // Name of the class that is calling this service
        intent.putExtra(ConnectionService.TAG_ACTIVITY_NAME, this.getClass().getName());
        // Starts the service
        startService(intent);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ReLoadConversationEvent event) {
        if(event!=null) {
             if (event.eventType.equalsIgnoreCase("Reconnecting")){
                if(isReconnecting){
                    isReconnecting = false;
                    txtStatus.setText("Reconnecting..");
                    LayoutReconnecting.setVisibility(View.VISIBLE);
                    isAlreadyConnected = true;
                }
            }
        }
    }


    public void stopIntervalOfConnection(){
        isSignalRConnected = true;
        if(timer!=null){
            timer.cancel();
        }
        runOnUiThread(() -> {
            if (!isReconnecting){
                isReconnecting = true;
                txtStatus.setText("Connected");
                Glide.with(mContext).load(R.drawable.wifi).into(icSource);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 100ms
                        LayoutReconnecting.setVisibility(View.GONE);
                        isAlreadyConnected = true;

                    }
                }, 1000);
            }
        });

    }

    public void setHubConnectionListeners(HubConnection hubConnection) {


        // list of messages
        hubConnection.on("ReceiveMessage", (message) -> {
            runOnUiThread(() -> {
                if (message != null) {
                    EventBus.getDefault().post(new ReceiveMessageEvent(message, "ReceiveMessage"));
                }
            });
        }, RecieveMessage.class);


        hubConnection.on("NewChatReceiver", (message) -> {
                runOnUiThread(() -> {
                    if (message != null) {
                        EventBus.getDefault().post(new NewChatRecieveResponse(message, "CallSendPrivateAfterNewChat"));
                    }
                });
            }, Conversation.class);


        // list of messages
        hubConnection.on("onError", (message) -> {
            runOnUiThread(() -> {
                if (message != null) {
                    EventBus.getDefault().post(new OnErrorData(message, "OnErrorMessage"));
                }
            });
        }, RecieveMessage.class);

    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (!event.eventType.isEmpty()) {
            if (event.eventType.equalsIgnoreCase("startSignalR")) {
                if (hubConnection != null && signalRHelper != null) {
                    boolean isSignalRConnect =  signalRHelper.startSignalRHubClient(hubConnection, agentId,mContext, ConnectMateMainActivity.this);
                    if(isSignalRConnect){
                        stopIntervalOfConnection();
                    }
                }
            }else if(event.eventType.equalsIgnoreCase("ShowToolbar")){
                   hideNshowToolbar(false);
            }else if (event.eventType.equalsIgnoreCase("HideToolbar")){
                   hideNshowToolbar(true);
            }else if (event.eventType.equalsIgnoreCase("SwitchToConversationList")){
                     finish();
            }else if (event.eventType.equalsIgnoreCase("isComingFromUserProfileDetailSwitch")){
                //ReplaceFragmentWithoutClearBackStack(new UserProfileFragment(),true,bundle,false);
            }
            else if (event.eventType.equalsIgnoreCase("Reload")){
                runOnUiThread(() -> {
                    if (txtStatus!=null){
                        txtStatus.setText("Connected");
                    }
                    if(icSource!=null){
                        Glide.with(mContext).load(R.drawable.wifi).into(icSource);
                    }
                    EventBus.getDefault().post(new ReLoadConversationEvent("ReloadConversationWhenConnect"));
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        //Do something after 100ms
                        LayoutReconnecting.setVisibility(View.GONE);
                        stopIntervalOfConnection();

                    }, 500);
                });
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(hubConnection!=null){
            hubConnection.remove("onError");
            hubConnection.remove("ReceiveMessage");
            hubConnection.remove("NewChatReceiver");
            hubConnection.stop();
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SendChatEvent event) {
        if(event!=null){
            if(event.eventType.equalsIgnoreCase("SendNewMessage")){
                try {
                    if(hubConnection!=null){
                        hubConnection.send("SendPrivateMessage",event.sendMessageModel);
                    }
                } catch (Exception e) {
                    if(e.getMessage().equalsIgnoreCase("The 'invoke' method cannot be called if the connection is not active.") || e.getMessage().equalsIgnoreCase("The 'send' method cannot be called if the connection is not active")|| e.getMessage().contains("is not active")){
                        if (hubConnection != null && signalRHelper != null) {
                            boolean isSignalRConnect =  signalRHelper.startSignalRHubClient(hubConnection, agentId,mContext, ConnectMateMainActivity.this);
                            if(isSignalRConnect){
                                stopIntervalOfConnection();
                            }
                        }
                    }
                }

            }
        }

    }

    @SuppressLint("CheckResult")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SendNewChatEvent event) {
        if(event!=null){
            if(event.eventType.equalsIgnoreCase("SendNewChatMessage")){
                try {
                    if(hubConnection!=null) {
                        if(hubConnection!=null){
                            hubConnection.send("NewChat",event.sendMessageModel);
                        }
                    }

                } catch (Exception e) {
                    if(e.getMessage().equalsIgnoreCase("The 'invoke' method cannot be called if the connection is not active.") || e.getMessage().equalsIgnoreCase("The 'send' method cannot be called if the connection is not active")|| e.getMessage().contains("is not active")){
                        if (hubConnection != null && signalRHelper != null) {
                            boolean isSignalRConnect =  signalRHelper.startSignalRHubClient(hubConnection, agentId,mContext, ConnectMateMainActivity.this);
                            if(isSignalRConnect){
                                stopIntervalOfConnection();
                            }
                        }
                    }
                }

            }
        }

    }




    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragInstance = fm.findFragmentById(R.id.main_content);
        if (!(fragInstance instanceof ConversationsDetailFragment)) {
            super.onBackPressed();
        }

    }

    public void hideNshowToolbar(boolean isHide){
        if(isHide){
            action_bar.setVisibility(View.GONE);
        }else{
            action_bar.setVisibility(View.VISIBLE);
        }

    }

    public void scheduleApiSignalRConnection() {
        int delay = 0; // delay for 0 sec.
        int period = 2500; // repeat every 10 sec.
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            public void run() {
                //Call function
                if(!isSignalRConnected) {
                    //ping signal are every 2 second once connect then upcoming
                    // Reconnect if the connection was lost due to an error
                    if (hubConnection != null  && signalRHelper != null) {
                        boolean isSignalRConnect =  signalRHelper.startSignalRHubClient(hubConnection, agentId,mContext, ConnectMateMainActivity.this);
                        if(isSignalRConnect){
                            stopIntervalOfConnection();
                        }
                    }
                }
            }
        }, delay, period);
    }

    @Override
    public void hasInternetConnection() {
        runOnUiThread(() -> {
            if (isAlreadyConnected){
                isAlreadyConnected = false;
                EventBus.getDefault().post(new MessageEvent("Reload"));
            }
        });
    }

    @Override
    public void hasNoInternetConnection() {
        runOnUiThread(() -> {
            isAlreadyConnected = true;
            EventBus.getDefault().post(new ReLoadConversationEvent("Reconnecting"));

        });
    }
    public void getAccessTokenByChannelId(String channelId){
        new ApiClient(mContext).getWebService().getAccessTokenByChannelId(channelId).enqueue(new Callback<WebResponse>() {
            @Override
            public void onResponse(Call<WebResponse> call, Response<WebResponse> response) {
                if (response.body()!=null){
                    if(response.isSuccessful()){
                        if (!response.body().getResult().toString().isEmpty()){
                            //create chathub connection
                            common.saveToken(ConnectMateMainActivity.this,response.body().getResult().toString());
                            hubConnection = signalRHelper.createChatHubConnection(response.body().getResult().toString(), mContext);
                            //set all chathub listners
                            setHubConnectionListeners(hubConnection);
                            //start signal R
                            if (hubConnection != null && signalRHelper != null) {
                                boolean isSignalRConnect =  signalRHelper.startSignalRHubClient(hubConnection, agentId,mContext, ConnectMateMainActivity.this);
                                if(isSignalRConnect){
                                    stopIntervalOfConnection();
                                }
                            }
                            hubConnection.onClosed(exception -> {
                                if (exception != null) {
                                    isSignalRConnected = false;
                                    scheduleApiSignalRConnection();
                                }
                            });

                        }

                    }
                }
            }

            @Override
            public void onFailure(Call<WebResponse> call, Throwable t) {

            }
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SendFileAfterPreview event) {
        if(event!=null) {
            if (event.eventType.equalsIgnoreCase("SendFileAfterPreview")) {
                Toast.makeText(mContext, "Please send File", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
