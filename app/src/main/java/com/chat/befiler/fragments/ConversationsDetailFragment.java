package com.chat.befiler.fragments;

import static com.chat.befiler.commons.Constants.COLOR_CODE_KEY;
import static com.chat.befiler.commons.Constants.HIDE_TOOLBAR;
import static com.chat.befiler.commons.Constants.NAME_KEY;
import static com.chat.befiler.commons.Constants.NAME_LETTER_KEY;
import static com.chat.befiler.commons.Constants.SOURCE_KEY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chat.befiler.Events.appEvents.FileDeleteEvent;
import com.chat.befiler.Events.appEvents.MessageEventFileDownload;
import com.chat.befiler.Events.chatEvents.AssignedChatEvent;
import com.chat.befiler.Events.chatEvents.ConversationEvent;
import com.chat.befiler.Events.appEvents.MessageEvent;
import com.chat.befiler.Events.chatEvents.ReceiveMessageEvent;
import com.chat.befiler.Events.chatEvents.SendChatEvent;
import com.chat.befiler.Events.chatEvents.SendFileAfterPreview;
import com.chat.befiler.Events.chatEvents.SendNewChatEvent;
import com.chat.befiler.activities.SelectFilePreviewActivity;
import com.chat.befiler.adapters.ConversationsByUIListAdapter;
import com.chat.befiler.adapters.ConversationsListingDetailAdapter;
import com.chat.befiler.adapters.FileDataClass;
import com.chat.befiler.adapters.SelectedFilesListAdapter;
import com.chat.befiler.commons.Common;
import com.chat.befiler.commons.Constants;
import com.chat.befiler.commons.FileUtil;
import com.chat.befiler.commons.PermissionHelper;
import com.chat.befiler.commons.Utils;
import com.chat.befiler.model.chat.AssignChatListener;
import com.chat.befiler.model.chat.Conversation;
import com.chat.befiler.model.chat.ConversationByUID;
import com.chat.befiler.model.chat.ConversationStatusListenerDataModel;
import com.chat.befiler.model.chat.FilesData;
import com.chat.befiler.model.chat.NewChatModel;
import com.chat.befiler.model.chat.NewChatRecieveResponse;
import com.chat.befiler.model.chat.OnErrorData;
import com.chat.befiler.model.chat.RecieveMessage;
import com.chat.befiler.model.chat.SendMessageModel;
import com.chat.befiler.model.chat.UploadFilesData;
import com.chat.befiler.retrofit.ApiClient;
import com.chat.befiler.retrofit.WebResponse;
import com.chat.befiler.retrofit.WebResponse2;
import com.download.library.DownloadImpl;
import com.download.library.DownloadListenerAdapter;
import com.download.library.Extra;
import com.example.signalrtestandroid.R;
import com.example.signalrtestandroid.databinding.FragmentConversationsDetailBinding;
import com.vanniktech.emoji.EmojiPopup;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import id.zelory.compressor.Compressor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */

public class ConversationsDetailFragment extends Fragment {
    ArrayList<UploadFilesData> uploadFilesData;
    ArrayList<FileDataClass> filesNames = null;
    ArrayList<MultipartBody.Part> filePart = null;
    File fileTemp = null;
    Handler handler = null;
    Runnable myRunnable = null;
    public String customerName = "";
    public long agentId = 0 ;
    public long cusId = -1;
    public long groupId = -1;
    public String mCurrentPhotoPath = "";
    int pageNumber = 1;
    int pageSize = 15;
    int totalPages = 1;
    public static boolean isCalledFromPreviewActivity = false;
    public static SendFileAfterPreview sendFileAfterPreview ;
    private boolean isLastPage = false;
    private boolean isLoading = false;
    //init layout manager for list
    LinearLayoutManager mLayoutManager;
    FragmentConversationsDetailBinding fragmentConversationsBinding;
    private int CAPTURE_PICTURE_FROM_CAMERA = 2;
    private int PICK_IMAGE_FOR_SELECT = 3;
    Common common;
    String conversationByUID = "";
    String tempChatId = "";
    int addedConversationPos = -1;
    Context mContext;
    ConversationsByUIListAdapter conversationsListAdapter = null;
    SelectedFilesListAdapter selectedFilesListAdapter = null;
    ArrayList<ConversationByUID> conversationArrayList ;
    ConversationsListingDetailAdapter conversationsListingDetailAdapter = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().post(new MessageEvent(HIDE_TOOLBAR));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        EventBus.getDefault().post(new MessageEvent(HIDE_TOOLBAR));
        mContext = context;
    }
    Conversation conversation = null;
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentConversationsBinding = FragmentConversationsDetailBinding.inflate(getLayoutInflater());
        View view = fragmentConversationsBinding.getRoot();
        UUID uuid = UUID.randomUUID();
        conversationByUID= uuid.toString();
        common = new Common();
        filesNames = new ArrayList<>();
        filePart = new ArrayList<>();
        uploadFilesData = new ArrayList<>();
        conversationArrayList = new ArrayList<>();
        conversation = common.getConversationData(mContext);
        final EmojiPopup popup = EmojiPopup.Builder
                .fromRootView(view).build(fragmentConversationsBinding.edtMessage);
        if (getArguments()!=null){
            Bundle bundle = getArguments();
            if(bundle.containsKey(NAME_LETTER_KEY) && bundle.containsKey(NAME_KEY) && bundle.containsKey(SOURCE_KEY) && bundle.containsKey(COLOR_CODE_KEY) && bundle.containsKey(Constants.CONVERSATION_BY_UID_KEY)){
                if (bundle.getString(NAME_LETTER_KEY)!=null){
                    fragmentConversationsBinding.txtNameFirstLetter.setText(bundle.getString(NAME_LETTER_KEY));
                }
//                if (bundle.getString(Constants.CONVERSATION_BY_UID_KEY)!=null){
//                    conversationByUID = bundle.getString(Constants.CONVERSATION_BY_UID_KEY);
//                }
                if (bundle.getString(SOURCE_KEY)!=null){
                    fragmentConversationsBinding.tvSource.setText(getString(R.string.fromlabel)+" "+bundle.getString(SOURCE_KEY));
                }
                if (bundle.getString(NAME_KEY)!=null){
                    fragmentConversationsBinding.tvName.setText(bundle.getString(NAME_KEY));
                }
                if (bundle.getString(COLOR_CODE_KEY)!=null){
                    if(!bundle.getString(COLOR_CODE_KEY).isEmpty()){
                        int colorCodeBg = Integer.parseInt(bundle.getString(COLOR_CODE_KEY));
                        fragmentConversationsBinding.ivFirstName.setColorFilter(colorCodeBg);
                    }
                }
            }
        }
        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, true);
        //mLayoutManager.setStackFromEnd(true);
        fragmentConversationsBinding.rvConversations.setLayoutManager(mLayoutManager);
        LinearLayoutManager layoutManagerSelectFiles = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, true);
        fragmentConversationsBinding.rvSelections.setLayoutManager(layoutManagerSelectFiles);

        fragmentConversationsBinding.menuClick.setOnClickListener(v -> {
            EventBus.getDefault().post(new MessageEvent("SwitchToConversationList"));
        });

        fragmentConversationsBinding.sendMessage.setOnClickListener(v -> {
//            uploadFilesData = new ArrayList<>();
//            if (!filesNames.isEmpty()){
//                fragmentConversationsBinding.ivSendMessage.setVisibility(View.GONE);
//                fragmentConversationsBinding.progressSend.setVisibility(View.VISIBLE);
//                if(filesNames.size()>0){
//                    for (int i=0;i<filesNames.size();i++){
//                        NewChatModel sendMessageModel = new NewChatModel();
//                        sendMessageModel.agentId = agentId;
//                        sendMessageModel.tempChatId = filesNames.get(i).tempChatId;
//                        sendMessageModel.conversationUId = conversationByUID;
//                        sendMessageModel.connectionId = common.getConnectionID(mContext);
//                        sendMessageModel.customerId = cusId ;
//                        sendMessageModel.name = customerName ;
//                        sendMessageModel.message = filesNames.get(i).fileName;
//                        sendMessageModel.contactNo =  "";
//                        sendMessageModel.source = "Mobile_Android" ;
//                        sendMessageModel.isFromWidget = true ;
//                        sendMessageModel.type = "file";
//                        sendMessageModel.channelid = "6901b42a-0776-41d2-ac76-6cb6f3029d53";
//                        sendMessageModel.notifyMessage = "";
//                        addTempItemToList(sendMessageModel);
//                        uploadFiles(conversationByUID,filePart,true,conversation,filesNames.get(i).tempChatId);
//                    }
//                } v
//            }else{
                if (!fragmentConversationsBinding.edtMessage.getText().toString().isEmpty()){
                    tempChatId = UUID.randomUUID().toString();
                    sendNewChat("text",fragmentConversationsBinding.edtMessage.getText().toString(),uploadFilesData,tempChatId);
                    fragmentConversationsBinding.edtMessage.setText("");
                }else{
                    Toast.makeText(getActivity(), "Please type a message", Toast.LENGTH_SHORT).show();
                }
//            }
        });
        fragmentConversationsBinding.layoutImageUpload.setOnClickListener(v -> {
            uploadImageDialog(getContext());
        });
        fragmentConversationsBinding.layoutFileAttach.setOnClickListener(v -> {
            storagePermission(false,true,null);

        });
        fragmentConversationsBinding.ivSmile.setOnClickListener(v -> {
            fragmentConversationsBinding.ivSmile.setVisibility(View.GONE);
            fragmentConversationsBinding.ivKeyboard.setVisibility(View.VISIBLE);
            popup.toggle();
        });
        fragmentConversationsBinding.ivKeyboard.setOnClickListener(v -> {
            fragmentConversationsBinding.ivKeyboard.setVisibility(View.GONE);
            fragmentConversationsBinding.ivSmile.setVisibility(View.VISIBLE);
            popup.dismiss();
        });

        //init layout manager for main list
        LinearLayoutManager layoutManagerList = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        fragmentConversationsBinding.rvConversationList.setLayoutManager(layoutManagerList);


        //api for all conversations
        //getConversationByUID(pageNumber,pageSize,conversationByUID,"0",false);

        KeyboardVisibilityEvent.setEventListener(getActivity(), isOpen -> {
            if (isOpen){
                fragmentConversationsBinding.ivSmile.setVisibility(View.GONE);
                fragmentConversationsBinding.ivKeyboard.setVisibility(View.VISIBLE);
            } else{
                fragmentConversationsBinding.ivSmile.setVisibility(View.VISIBLE);
                fragmentConversationsBinding.ivKeyboard.setVisibility(View.GONE);
            }

        });


        /**
         * add scroll listener while user reach in bottom load more will call
         */
      /*  fragmentConversationsBinding.rvConversations.addOnScrollListener(new PaginationScrollListener(mLayoutManager) {
            @Override
            protected void loadMoreItems() {
                // check weather is last page or not
                if (pageNumber<=totalPages){
                    pageNumber++;
                    isLoading = true;
                    fragmentConversationsBinding.loadMoreProgress.setVisibility(View.VISIBLE);
//                    getAllConversation(pageNumber,pageSize,common.getUserId(mContext),common.getIsSuperAdmin(mContext),se,false);
                    //api for all conversations
//                    getConversationByUID(pageNumber,pageSize,conversationByUID,common.getUserId(mContext),false);
                    //getConversationByUID(pageNumber,pageSize,conversationByUID,"0",false);
                } else {
                    isLoading = false;
                    isLastPage = true;
                }
            }
            @Override
            public boolean isLastPage() {
                return isLastPage;
            }
            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });*/

        fragmentConversationsBinding.jumptoBottom.setOnClickListener(view1 -> {
            scrollToBottom();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().post(new MessageEvent(HIDE_TOOLBAR));
        if (isCalledFromPreviewActivity){
            isCalledFromPreviewActivity = false;
            if(sendFileAfterPreview!=null){
                if(sendFileAfterPreview.eventType.equalsIgnoreCase("SendFileAfterPreview")){
                    if(filesNames.size()>0){
                        NewChatModel sendMessageModel = new NewChatModel();
                        sendMessageModel.agentId = agentId;
                        sendMessageModel.tempChatId = filesNames.get(0).tempChatId;
                        sendMessageModel.conversationUId = conversationByUID;
                        sendMessageModel.connectionId = common.getConnectionID(mContext);
                        sendMessageModel.customerId = cusId ;
                        sendMessageModel.name = customerName ;
                        sendMessageModel.message = filesNames.get(0).fileName;
                        sendMessageModel.contactNo =  "";
                        sendMessageModel.source = "Mobile_Android" ;
                        sendMessageModel.isFromWidget = true ;
                        sendMessageModel.type = "file";
                        sendMessageModel.channelid = "6901b42a-0776-41d2-ac76-6cb6f3029d53";
                        sendMessageModel.notifyMessage = "";
                        addTempItemToList(sendMessageModel);
                        uploadFiles(conversationByUID,filePart,true,conversation,filesNames.get(0).tempChatId);
                }

                }
            }
        }

    }

    public void getConversationByUID(int pageNumber,int pageSize,String conversationByUID,String customerId,boolean isCalledFromAssigned) {
        if(pageNumber==1){
            if (isAdded()){
                if(getActivity()!=null){
                    getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
        }
        new ApiClient(getContext()).getWebService().getConversationByUID(pageNumber,pageSize,conversationByUID,customerId).enqueue(new Callback<WebResponse2<ArrayList<ConversationByUID>>>() {
            @Override
            public void onResponse(Call<WebResponse2<ArrayList<ConversationByUID>>> call, Response<WebResponse2<ArrayList<ConversationByUID>>> response) {
                if(response.code()==200){
                    if(response.body().getResult().size()>0){
                        totalPages = response.body().getTotalPages();
                        if(pageNumber==1){
                            if(response.body().getResult().size()>0) {
                                conversationArrayList = response.body().getResult();
                                conversationsListAdapter = new ConversationsByUIListAdapter(mContext,conversationArrayList);
                                fragmentConversationsBinding.rvConversations.setAdapter(conversationsListAdapter);
                                scrollToBottom();
                            }
                        }else {
                            // check weather is last page or not
                            if (pageNumber <totalPages) {
                                //show loader here
                                    fragmentConversationsBinding.loadMoreProgress.setVisibility(View.VISIBLE);
                            } else {
                                isLastPage = true;
                            }
                            fragmentConversationsBinding .jumptoBottom.setVisibility(View.VISIBLE);
                            isLoading = false;
                            if(response.body().getResult().size()>0){
                                conversationArrayList.addAll(response.body().getResult());
                                conversationsListAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                } else{
                    if(response.code()==401){
//                        switch to login
                    }
                }
                if (isAdded()){
                    if(getActivity()!=null){
                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                }
                handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() ->
                                fragmentConversationsBinding.loadMoreProgress.setVisibility(View.GONE)
                        , 500);
            }

            @Override
            public void onFailure(Call<WebResponse2<ArrayList<ConversationByUID>>> call, Throwable t) {
                if (isAdded()){
                    if(getActivity()!=null){
                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                }
                if (t.getMessage().contains("Failed to connect")){
                    EventBus.getDefault().post(new ReLoadConversationEvent("Reconnecting"));
                }
            }
        });
    }

    public void uploadFiles(String conversationByUID,ArrayList<MultipartBody.Part> partArrayList,boolean isNewChat,Conversation conversationn,String tempChatID){
        if (getActivity().getWindow() != null) {
            getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
        new ApiClient(mContext).getWebService().uploadFiles(conversationByUID,partArrayList).enqueue(new Callback<WebResponse<ArrayList<UploadFilesData>>>() {
            @Override
            public void onResponse(Call<WebResponse<ArrayList<UploadFilesData>>> call, Response<WebResponse<ArrayList<UploadFilesData>>> response) {
                if (response!=null){
                    if(response.code()==200){
                        if(response.isSuccessful() && response.body()!=null){
                            if(response.body().getResult()!=null){
                                uploadFilesData = response.body().getResult();
                                //send message here
                                if(uploadFilesData.size()>0){
                                        if (isNewChat){
                                            sendNewChat("file",fragmentConversationsBinding.edtMessage.getText().toString(),uploadFilesData,tempChatID);
                                            fragmentConversationsBinding.edtMessage.setText("");
                                        }else{
                                            sendMessage(conversationn,"file",fragmentConversationsBinding.edtMessage.getText().toString(),uploadFilesData);
                                            fragmentConversationsBinding.edtMessage.setText("");
                                        }
                                        hideSelectedFilesLayout();
                                }

                            }
                        }
                    }else{
                        if (response.code()==401){

                        }else{

                        }
                    }

                }
                if(getActivity().getWindow()!=null){
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
            @Override
            public void onFailure(Call<WebResponse<ArrayList<UploadFilesData>>> call, Throwable t) {
                if(getActivity().getWindow()!=null){
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
                if (t.getMessage().contains("Failed to connect")){
                    EventBus.getDefault().post(new ReLoadConversationEvent("Reconnecting"));
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void hideSelectedFilesLayout(){
        if(!filesNames.isEmpty()){
            filesNames.clear();
        }
        if(!filePart.isEmpty()){
            filePart.clear();
        }
        fragmentConversationsBinding.layoutSelectedFiles.setVisibility(View.GONE);
    }
    public void scrollToBottom(){
        fragmentConversationsBinding .jumptoBottom.setVisibility(View.GONE);
        fragmentConversationsBinding.rvConversations.postDelayed(() -> fragmentConversationsBinding.rvConversations.scrollToPosition(0), 200);
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
    public void onMessageEvent(AssignedChatEvent event) {
        if(event!=null){
            if(event.eventType.equalsIgnoreCase("AssignChatToAnother")){
                if (event.assignChatListener.conversationUId.equalsIgnoreCase(conversationByUID)){
                    fragmentConversationsBinding.layoutTyping.setVisibility(View.GONE);
                    //Remove item becuase it assigned to another agent by admin side
                    if(conversationsListAdapter!=null && !conversationArrayList.isEmpty() && fragmentConversationsBinding!=null) {
                        if (isContainAssignChat(conversationArrayList, event.assignChatListener)) {
                            conversationArrayList.add(fragmentConversationsBinding.rvConversations.getAdapter().getItemCount(), getConversationFromAssignListener(event.assignChatListener));
                            conversationsListAdapter.notifyItemInserted(fragmentConversationsBinding.rvConversations.getAdapter().getItemCount());
                            scrollToBottom();
                        } else {
                            conversationArrayList.add(getConversationFromAssignListener(event.assignChatListener));
                            conversationsListAdapter = new ConversationsByUIListAdapter(mContext, conversationArrayList);
                            fragmentConversationsBinding.rvConversations.setAdapter(conversationsListAdapter);
                        }
                    }
                }else{
                    fragmentConversationsBinding.layoutTyping.setVisibility(View.VISIBLE);
                }
                handler = new Handler();
                myRunnable = () -> {

                    EventBus.getDefault().post(new MessageEvent("SwitchToConversationList"));

                };
                handler.postDelayed(myRunnable, 1500);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(FileDeleteEvent event) {
        if (!event.eventType.isEmpty()) {
            if (event.eventType.equalsIgnoreCase("DeleteFile")) {
                if(event.position!=-1){
                    if(!filesNames.isEmpty() && !filePart.isEmpty()){
                        filesNames.remove(event.position);
                        filePart.remove(event.position);
                        if(selectedFilesListAdapter!=null){
                            selectedFilesListAdapter.notifyItemRemoved(event.position);
                        }
                        if(filesNames.isEmpty() && filePart.isEmpty()){
                            hideSelectedFilesLayout();
                        }
                    }

                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEventFileDownload event) {
        if (!event.eventType.isEmpty()) {
            if (event.eventType.equalsIgnoreCase("FileDownload")) {
                storagePermission(event);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ReceiveMessageEvent event) {
        if (!event.eventType.isEmpty()) {
            fragmentConversationsBinding.ivSendMessage.setVisibility(View.VISIBLE);
            fragmentConversationsBinding.progressSend.setVisibility(View.GONE);
            if(!uploadFilesData.isEmpty()){
                uploadFilesData.clear();
            }
            if (event.eventType.equalsIgnoreCase("ReceiveMessage")) {
                if(conversationByUID.equalsIgnoreCase(event.recieveMessage.conversationUid)){
                    if(conversationsListAdapter!=null && !conversationArrayList.isEmpty() && fragmentConversationsBinding!=null){
                        if (conversationByUID.equalsIgnoreCase(event.recieveMessage.conversationUid)){
                            if (isContainConversationByUIdTemp(conversationArrayList,event.recieveMessage.tempChatId)){
                                ConversationByUID conversationByUID1  = getConversationFromReceiveMsg(event.recieveMessage);
                                conversationByUID1.isUpdateStatus = true;
                                conversationArrayList.set(addedConversationPos,conversationByUID1);
                                conversationsListAdapter.notifyItemChanged(addedConversationPos);
                                scrollToBottom();
                            }else{
                                conversationArrayList.add(0, getConversationFromReceiveMsg(event.recieveMessage));
                                conversationsListAdapter.notifyItemInserted(0);
                                scrollToBottom();
                            }
                        }else{
                            conversationArrayList.add(getConversationFromReceiveMsg(event.recieveMessage));
                            conversationsListAdapter = new ConversationsByUIListAdapter(mContext,conversationArrayList);
                            fragmentConversationsBinding.rvConversations.setAdapter(conversationsListAdapter);
                        }
                    }else{
                        conversationArrayList.add(getConversationFromReceiveMsg(event.recieveMessage));
                        conversationsListAdapter = new ConversationsByUIListAdapter(mContext,conversationArrayList);
                        fragmentConversationsBinding.rvConversations.setAdapter(conversationsListAdapter);
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(OnErrorData event) {
        if (!event.eventType.isEmpty()) {
            if (event.eventType.equalsIgnoreCase("OnErrorMessage")) {
                    if (event.recieveMessage!=null){
                        if(event.recieveMessage.content!=null && !event.recieveMessage.content.isEmpty()){
                            Toast.makeText(mContext, event.recieveMessage.content, Toast.LENGTH_SHORT).show();
                        }
                    }
            }
        }
    }

    public ConversationByUID getConversationFromResolveListener(ConversationStatusListenerDataModel conversationStatusListenerDataModel){
        Conversation conversation = null;
        ConversationByUID conversationByUID = null;
        if (common.getConversationData(mContext)!=null){
             conversation = common.getConversationData(mContext);
        }
        if (conversation!=null && conversationStatusListenerDataModel!=null){
            conversationByUID = new ConversationByUID();
            conversationByUID.conversationType = "text";
            conversationByUID.type = "system";
            conversationByUID.conversationUid = conversation.conversationUid;
            conversationByUID.toUserId = 0;
            conversationByUID.customerName = "";
            conversationByUID.sender = "";
            conversationByUID.agentId = 0;
            conversationByUID.customerId = conversation.customerId;
            conversationByUID.content = conversationStatusListenerDataModel.notifyMessage;
            conversationByUID.files = new ArrayList<>();
            conversationByUID.fromUserId = 0;
            conversationByUID.isFromWidget = false;
            conversationByUID.isPrivate = false;
            conversationByUID.groupId = conversation.groupId;
            conversationByUID.groupName = conversation.groupName;
            conversationByUID.receiver = "";
            conversationByUID.pageId = "";
            conversationByUID.pageName = "";
            conversationByUID.tiggerevent = 0;
            conversationByUID.timestamp = conversationStatusListenerDataModel.timestamp;
        }

        return conversationByUID;
    }

    public ConversationByUID getConversationFromAssignListener(AssignChatListener assignChatListener){
        ConversationByUID conversationByUID = new ConversationByUID();
        conversationByUID.conversationType = "text";
        conversationByUID.type = "system";
        conversationByUID.conversationUid = assignChatListener.conversationUId;
        conversationByUID.toUserId = 0;
        conversationByUID.customerName = "";
        conversationByUID.sender = "";
        conversationByUID.agentId = 0;
        conversationByUID.customerId = assignChatListener.customerId;
        conversationByUID.content = assignChatListener.notifyMessage;
        conversationByUID.files = new ArrayList<>();
        conversationByUID.fromUserId = 0;
        conversationByUID.isFromWidget = false;
        conversationByUID.isPrivate = false;
        conversationByUID.groupId = assignChatListener.groupId;
        conversationByUID.groupName = assignChatListener.groupName;
        conversationByUID.timestamp = assignChatListener.timestamp;
        conversationByUID.receiver = "";
        conversationByUID.pageId = "";
        conversationByUID.pageName = "";
        conversationByUID.tiggerevent = 0;

        return conversationByUID;
    }
    public ConversationByUID addSendMessageTemp(NewChatModel recieveMessage){
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
        if (recieveMessage.type.equalsIgnoreCase("file")){
            ArrayList<FilesData> arrayList = new ArrayList<>();
            ConversationByUID conversation = new ConversationByUID();
            conversation.type = recieveMessage.type;
            conversation.conversationType = recieveMessage.type.equalsIgnoreCase("file") ? "multimedia" : "text";
            conversation.conversationUid = recieveMessage.conversationUId;
            conversation.toUserId = 0;
            conversation.customerName = recieveMessage.name;
            conversation.sender = "";
            conversation.agentId = recieveMessage.agentId;
            conversation.customerId = recieveMessage.customerId;
            conversation.content = recieveMessage.message;
            FileDataClass fileDataClass = (FileDataClass) filesNames.get(0);
            FilesData filesData = new FilesData();
            filesData.url = fileDataClass.url;
            filesData.type = fileDataClass.mimeType;
            filesData.documentName = fileDataClass.fileName;
            filesData.isLocalFile = true;
            arrayList.add(filesData);
            conversation.files = arrayList;
            conversation.fromUserId = 0;
            conversation.isFromWidget = recieveMessage.isFromWidget;
            conversation.isPrivate = false;
            conversation.groupId = groupId;
            conversation.groupName = "";
            conversation.timestamp = timeStamp;
            conversation.receiver = "";
            conversation.pageId = "";
            conversation.pageName = "";
            conversation.tiggerevent = 0;
            conversation.tempChatId = recieveMessage.tempChatId;
            conversation.isNotNewChat = false;
            conversation.isUpdateStatus = false;
            conversation.isShowLocalFiles = true;
            fragmentConversationsBinding.ivSendMessage.setVisibility(View.VISIBLE);
            fragmentConversationsBinding.progressSend.setVisibility(View.GONE);
            fragmentConversationsBinding.layoutSelectedFiles.setVisibility(View.GONE);
            return conversation;
        }else{
            ConversationByUID conversation = new ConversationByUID();
            conversation.conversationType = recieveMessage.type.equalsIgnoreCase("file") ? "multimedia" : "text";
            conversation.type = recieveMessage.type;
            conversation.conversationUid = recieveMessage.conversationUId;
            conversation.toUserId = 0;
            conversation.customerName = recieveMessage.name;
            conversation.sender = "";
            conversation.agentId = recieveMessage.agentId;
            conversation.customerId = recieveMessage.customerId;
            conversation.content = recieveMessage.message;
            conversation.fromUserId = 0;
            conversation.isFromWidget = recieveMessage.isFromWidget;
            conversation.isPrivate = false;
            conversation.groupId = groupId;
            conversation.groupName = "";
            conversation.timestamp = timeStamp;
            conversation.receiver = "";
            conversation.pageId = "";
            conversation.pageName = "";
            conversation.tiggerevent = 0;
            conversation.tempChatId = tempChatId;
            conversation.isNotNewChat = false;
            conversation.isUpdateStatus = false;
            conversation.isShowLocalFiles = false;

            return conversation;
        }


    }

    public ConversationByUID getConversationFromReceiveMsg(RecieveMessage recieveMessage){
        ConversationByUID conversationByUID = new ConversationByUID();
        conversationByUID.conversationType = recieveMessage.type.equalsIgnoreCase("file") ? "multimedia" : "text";
        conversationByUID.type = recieveMessage.type;
        conversationByUID.conversationUid = recieveMessage.conversationUid;
        conversationByUID.toUserId = recieveMessage.toUserId;
        conversationByUID.customerName = recieveMessage.customerName;
        conversationByUID.sender = recieveMessage.sender;
        conversationByUID.agentId = recieveMessage.agentId;
        conversationByUID.customerId = recieveMessage.customerId;
        conversationByUID.content = recieveMessage.content;
        conversationByUID.files = recieveMessage.files;
        conversationByUID.fromUserId = recieveMessage.fromUserId;
        conversationByUID.isFromWidget = recieveMessage.isFromWidget;
        conversationByUID.isPrivate = recieveMessage.isPrivate;
        conversationByUID.groupId = recieveMessage.groupId;
        conversationByUID.groupName = recieveMessage.groupName;
        conversationByUID.timestamp = recieveMessage.timestamp;
        conversationByUID.receiver = recieveMessage.receiver;
        conversationByUID.pageId = recieveMessage.pageId;
        conversationByUID.pageName = recieveMessage.pageName;
        conversationByUID.tiggerevent = recieveMessage.tiggerevent;
        conversationByUID.isUpdateStatus = false;
        conversationByUID.isNotNewChat = true;
        return conversationByUID;
    }

    public boolean isContainReAssignChat(ArrayList<ConversationByUID> conversationArrayList, Conversation conversation){
        for (int i=0;i<conversationArrayList.size();i++){
            if(conversationArrayList.get(i).customerId == conversation.customerId){
                addedConversationPos = i;
                return true;
            }
        }
        return false;
    }
    public boolean isContainAssignChat(ArrayList<ConversationByUID> conversationArrayList, AssignChatListener assignChatListener){
        for (int i=0;i<conversationArrayList.size();i++){
            if(conversationArrayList.get(i).customerId == assignChatListener.customerId){
                addedConversationPos = i;
                return true;
            }
        }
        return false;
    }
    public boolean isContainConversationByUId(ArrayList<ConversationByUID> conversationArrayList, RecieveMessage conversation){
        for (int i=0;i<conversationArrayList.size();i++){
            if(conversationArrayList.get(i).conversationUid == conversation.conversationUid){
                addedConversationPos = i;
                return true;
            }
        }
        return false;
    }

    public boolean isContainConversationByUIdTemp(ArrayList<ConversationByUID> conversationArrayList, String tempChatId){
        for (int i=0;i<conversationArrayList.size();i++){
            if(conversationArrayList.get(i).tempChatId.equalsIgnoreCase(tempChatId)){
                addedConversationPos = i;
                return true;
            }
        }
        return false;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        common.setIsUnAssign(mContext,false);
        EventBus.getDefault().post(new MessageEvent("ShowToolbar"));
    }

    public void sendNewChat(String type,String txtMessage,ArrayList<UploadFilesData> uploadFilesData,String tempChatID){
        if (type.equalsIgnoreCase("file")){
            if(uploadFilesData.size()>0){
                    NewChatModel sendMessageModel = new NewChatModel();
                    sendMessageModel.agentId = agentId;
                    sendMessageModel.tempChatId = tempChatID;
                    sendMessageModel.conversationUId = conversationByUID;
                    sendMessageModel.connectionId = common.getConnectionID(mContext);
                    sendMessageModel.customerId = cusId ;
                    sendMessageModel.name = customerName ;
                    sendMessageModel.message = uploadFilesData.get(0).documentOrignalName;
                    sendMessageModel.contactNo =  "";
                    sendMessageModel.source = "Mobile_Android" ;
                    sendMessageModel.isFromWidget = true ;
                    sendMessageModel.type = type;
                    sendMessageModel.channelid = "6901b42a-0776-41d2-ac76-6cb6f3029d53";
                    sendMessageModel.notifyMessage = "";
                    sendMessageModel.documentName = uploadFilesData.get(0).documentName;
                    sendMessageModel.documentOrignalname = uploadFilesData.get(0).documentOrignalName;
                    sendMessageModel.documentType = uploadFilesData.get(0).documentType;
                    EventBus.getDefault().post(new SendNewChatEvent(sendMessageModel,"SendNewChatMessage"));
            }
        }else {
            NewChatModel sendMessageModel = new NewChatModel();
            sendMessageModel.agentId = agentId;
            sendMessageModel.tempChatId = tempChatID;
            sendMessageModel.conversationUId = conversationByUID;
            sendMessageModel.connectionId = common.getConnectionID(mContext);
            sendMessageModel.customerId = cusId;
            sendMessageModel.name = customerName;
            sendMessageModel.message = txtMessage;
            sendMessageModel.contactNo = "";
            sendMessageModel.source = "Mobile_Android";
            sendMessageModel.isFromWidget = true;
            sendMessageModel.type = type;
            sendMessageModel.channelid = "6901b42a-0776-41d2-ac76-6cb6f3029d53";
            sendMessageModel.notifyMessage = "";
            sendMessageModel.documentName = "";
            sendMessageModel.documentOrignalname = "";
            sendMessageModel.documentType = "";
            EventBus.getDefault().post(new SendNewChatEvent(sendMessageModel, "SendNewChatMessage"));
            addTempItemToList(sendMessageModel);

        }
    }
    public void addTempItemToList(NewChatModel sendMessageModel){
        if (conversationArrayList.isEmpty()) {
            conversationArrayList.add(addSendMessageTemp(sendMessageModel));
            conversationsListAdapter = new ConversationsByUIListAdapter(mContext,conversationArrayList);
            fragmentConversationsBinding.rvConversations.setAdapter(conversationsListAdapter);
        }else{
            conversationArrayList.add(0, addSendMessageTemp(sendMessageModel));
            conversationsListAdapter.notifyItemInserted(0);
            scrollToBottom();
        }
    }

    public void sendMessage(Conversation conversation,String type,String txtMessage,ArrayList<UploadFilesData> uploadFilesData){
        if (type.equalsIgnoreCase("file")){
            if(uploadFilesData.size()>0){
                    SendMessageModel sendMessageModel = new SendMessageModel();
                    sendMessageModel.tempChatId = conversation.tempChatId;
                    sendMessageModel.agentId = conversation.agentId;
                    sendMessageModel.conversationUid = conversation.conversationUid ;
                    sendMessageModel.conversationId = conversation.conversationUid;
                    sendMessageModel.customerId = conversation.customerId ;
                    sendMessageModel.message = uploadFilesData.get(0).documentOrignalName;
                    sendMessageModel.receiverConnectionId =  common.getConnectionID(mContext);
                    sendMessageModel.receiverName = conversation.customerName ;
                    sendMessageModel.isFromWidget = true ;
                    sendMessageModel.type = type;
                    sendMessageModel.groupId = conversation.groupId;
                    sendMessageModel.conversationType = type.equalsIgnoreCase("file") ? "multimedia" : "text";
                    sendMessageModel.documentName = uploadFilesData.get(0).documentName;
                    sendMessageModel.documentOrignalname = uploadFilesData.get(0).documentOrignalName ;
                    sendMessageModel.documentType = uploadFilesData.get(0).documentType ;
                    sendMessageModel.icon = "" ;
                    sendMessageModel.pageId = "" ;
                    sendMessageModel.pageName = "" ;
                    EventBus.getDefault().post(new SendChatEvent(sendMessageModel,"SendNewMessage"));
//                    if (uploadFilesData.size()-1 == i){
//                        fragmentConversationsBinding.ivSendMessage.setVisibility(View.VISIBLE);
//                        fragmentConversationsBinding.progressSend.setVisibility(View.GONE);
//                    }
            }
        }else{
            SendMessageModel sendMessageModel = new SendMessageModel();
            sendMessageModel.customerId = conversation.customerId ;
            sendMessageModel.tempChatId = tempChatId;
            sendMessageModel.agentId = conversation.agentId;
            sendMessageModel.conversationUid = conversation.conversationUid ;
            sendMessageModel.conversationId = conversation.conversationUid ;
            sendMessageModel.message = txtMessage;
            sendMessageModel.receiverConnectionId =  common.getConnectionID(mContext);
            sendMessageModel.receiverName = conversation.customerName;
            sendMessageModel.isFromWidget = true ;
            sendMessageModel.type = type;
            sendMessageModel.groupId = conversation.groupId;
            sendMessageModel.conversationType = type.equalsIgnoreCase("file") ? "multimedia" : "text";
            sendMessageModel.documentOrignalname = "" ;
            sendMessageModel.documentName = "";
            sendMessageModel.documentType = "" ;
            sendMessageModel.icon = "" ;
            sendMessageModel.pageId = "" ;
            sendMessageModel.pageName = "" ;
            EventBus.getDefault().post(new SendChatEvent(sendMessageModel,"SendNewMessage"));
        }
    }
    private void uploadImageDialog(final Context context) {
        Dialog dialog = new Dialog(context);
        View newUserView;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        newUserView = inflater.inflate(R.layout.upload_img_dialoge, null);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        newUserView.setBackground(ContextCompat.getDrawable(context, R.drawable.round_border_rectangle));
        dialog.setContentView(newUserView);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        //show dialog
        if(dialog.isShowing()){
            dialog.dismiss();
        }
        ImageView btnSelectImg = newUserView.findViewById(R.id.btnSelectImg);
        ImageView btnCaptureImg = newUserView.findViewById(R.id.btnCaptureImg);
        ImageView btnCancel = newUserView.findViewById(R.id.btnCancel);

        btnSelectImg.setOnClickListener(view -> {
            // select image from gallery
            storagePermission(true,false,dialog);
        });

        btnCaptureImg.setOnClickListener(view -> {
            // open camera
            storagePermission(false,false,dialog);
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        dialog.show();

    }

    private void storagePermission(MessageEventFileDownload messageEventFileDownload){
        ArrayList<String> permissionList = new ArrayList<>();
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        PermissionHelper.grantMultiplePermissions(getActivity(), permissionList, new PermissionHelper.PermissionInterface() {
            @Override
            public void onSuccess() {
                if (!messageEventFileDownload.files.isEmpty()){
                    if (!messageEventFileDownload.files.get(0).url.isEmpty()) {
                        String rootPath = Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/BefilerChat/";
                        File root = new File(rootPath);
                        if (!root.exists()) {
                            root.mkdirs();
                        }
                        File f = new File(rootPath + messageEventFileDownload.documentName);
                        if (f.exists()) {
                            //open file here
                            try {
                                common.openFile(mContext,f);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{
                            if (messageEventFileDownload.files.get(0).isLocalFile){
                                ConversationByUID conversationByUID = messageEventFileDownload.conversationByUID;
                                if (messageEventFileDownload.conversationByUID!=null){
                                    conversationByUID.isShowLocalFiles = true;
                                    if(conversationsListAdapter!=null && messageEventFileDownload.position!=-1 && conversationsListAdapter.getItemCount()>0){
                                        conversationsListAdapter.notifyItemChanged(messageEventFileDownload.position,conversationByUID);
                                    }
                                }
                            }else{
                                ConversationByUID conversationByUID = messageEventFileDownload.conversationByUID;
                                DownloadImpl.getInstance(mContext)
                                        .url(messageEventFileDownload.files.get(0).url).target(f)
                                        .enqueue(new DownloadListenerAdapter() {
                                            @Override
                                            public void onStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength, Extra extra) {
                                                super.onStart(url, userAgent, contentDisposition, mimetype, contentLength, extra);
                                                if (messageEventFileDownload.conversationByUID!=null){
                                                    conversationByUID.isDownloading = true;
                                                    conversationByUID.isShowLocalFiles = false;
                                                    if(conversationsListAdapter!=null && messageEventFileDownload.position!=-1 && conversationsListAdapter.getItemCount()>0){
                                                        conversationsListAdapter.notifyItemChanged(messageEventFileDownload.position,conversationByUID);
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onProgress(String url, long downloaded, long length, long usedTime) {
                                                super.onProgress(url, downloaded, length, usedTime);
                                                //Log.i("TAG", " progress:" + downloaded + " url:" + url);
                                            }

                                            @Override
                                            public boolean onResult(Throwable throwable, Uri path, String url, Extra extra) {

                                                handler = new Handler();
                                                myRunnable = () -> {
                                                    // Things to be done
                                                    try {
                                                        if (messageEventFileDownload.conversationByUID!=null){
                                                            conversationByUID.isDownloading = false;
                                                            if(conversationsListAdapter!=null && messageEventFileDownload.position!=-1 && conversationsListAdapter.getItemCount()>0){
                                                                conversationsListAdapter.notifyItemChanged(messageEventFileDownload.position,conversationByUID);
                                                            }
                                                        }
                                                        if(common!=null){
                                                            common.openFile(mContext,new File(path.getPath()));
                                                        }
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                };
                                                handler.postDelayed(myRunnable, 1200);
                                                //Toast.makeText(mContext, "File Saved", Toast.LENGTH_SHORT).show();
                                                return super.onResult(throwable, path, url, extra);
                                            }
                                        });
                            }


                        }
                    }
                }

            }

            @Override
            public void onError() {
                storagePermission(messageEventFileDownload);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().post(new MessageEvent("ShowToolbar"));
        if(handler!=null){
            handler.removeCallbacks(myRunnable);
        }
    }

    private void storagePermission(boolean openGalleryStatus, boolean isFileAttach, Dialog dialog) {
        ArrayList<String> permissionList = new ArrayList<>();
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionList.add(Manifest.permission.CAMERA);
        PermissionHelper.grantMultiplePermissions(getActivity(), permissionList, new PermissionHelper.PermissionInterface() {
            @Override
            public void onSuccess() {
                if (isFileAttach){
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    String [] mimeTypes = {"image/*","application/msword","application/doc","application/docx", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    startActivityForResult(intent, PICK_IMAGE_FOR_SELECT);
                }
                else if (openGalleryStatus) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    String[] mimeTypes = {"image/*"};
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    startActivityForResult(intent, PICK_IMAGE_FOR_SELECT);
                    if(dialog!=null&& dialog.isShowing()){
                        dialog.dismiss();
                    }

                } else {
                    if(dialog!=null&& dialog.isShowing()){
                        dialog.dismiss();
                    }
                    dispatchTakePictureIntent(CAPTURE_PICTURE_FROM_CAMERA);
                }

            }

            @Override
            public void onError() {

            }
        });
    }

    private void dispatchTakePictureIntent(int requearCode) {
        if (mContext!=null){
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(mContext.getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI;
                    if (Build.VERSION.SDK_INT >= 24) {
                        photoURI = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", photoFile);
                    }else{
                        photoURI = Uri.fromFile(photoFile);
                    }
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(takePictureIntent, requearCode);
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String fileUri="";
        String fileName="";
        String fileType="";
        switch (requestCode) {
            case 3:
                boolean isFileSizeExceed = false;
                if (resultCode == -1) {
                    // Checking whether data is null or not
                    if (data != null) {
                        // Checking for selection multiple files or single.
                        if (data.getClipData() != null) {
                            // Getting the length of data and logging up the logs using index
                            for (int index = 0; index < data.getClipData().getItemCount(); index++) {
                                // Getting the URIs of the selected files and logging them into logcat at debug level
                                Uri uri = data.getClipData().getItemAt(index).getUri();
                                if (mContext.getContentResolver().getType(uri).equalsIgnoreCase("image/jpeg") ||
                                        mContext.getContentResolver().getType(uri).equalsIgnoreCase("image/jpg") ||
                                        mContext.getContentResolver().getType(uri).equalsIgnoreCase("image/png")) {
                                    File compressedImageFile = null;
                                    try {
                                        try {
                                            fileTemp = FileUtil.from(mContext, uri);
                                            Log.d("file", "File...:::: uti - " + fileTemp.getPath() + " file -" + fileTemp + " : " + fileTemp.exists());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        compressedImageFile = new Compressor(mContext).compressToFile(fileTemp);
                                        filePart.add(MultipartBody.Part.createFormData("files", compressedImageFile.getName(),
                                                RequestBody.create(MediaType.parse("*/*"), compressedImageFile)));
                                        filesNames.add(new FileDataClass(compressedImageFile.getName(),common.getFolderSizeLabel(compressedImageFile),common.getMimeType(Uri.fromFile(compressedImageFile),getContext()),uri.toString(),UUID.randomUUID().toString()));
                                        fileUri = uri.toString();
                                        fileType = common.getMimeType(Uri.fromFile(fileTemp),mContext);
                                        fileName = compressedImageFile.getName();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }


                                } else {
                                    if (Utils.isCheckFileSize(uri, mContext)) {
                                        File compressedImageFile = null;
                                        try {
                                            fileTemp = FileUtil.from(mContext, uri);
                                            Log.d("file", "File...:::: uti - " + fileTemp.getPath() + " file -" + fileTemp + " : " + fileTemp.exists());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        filePart.add(MultipartBody.Part.createFormData("files", fileTemp.getName(),
                                                RequestBody.create(MediaType.parse("*/*"), fileTemp)));
                                        filesNames.add(new FileDataClass(fileTemp.getName(),common.getFolderSizeLabel(fileTemp),common.getMimeType(Uri.fromFile(fileTemp),getContext()),uri.toString(),UUID.randomUUID().toString()));
                                        fileUri = uri.toString();
                                        fileType = common.getMimeType(Uri.fromFile(fileTemp),mContext);
                                        fileName = fileTemp.getName();
                                    } else {
                                        Toast.makeText(mContext, "File size exceeded from 2.5 mb", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            }
                        } else {
                            // Getting the URI of the selected file and logging into logcat at debug level
                            Uri uri = data.getData();
                            if (mContext.getContentResolver().getType(uri).equalsIgnoreCase("image/jpeg") ||
                                    mContext.getContentResolver().getType(uri).equalsIgnoreCase("image/jpg") ||
                                    mContext.getContentResolver().getType(uri).equalsIgnoreCase("image/png")) {
                                //uris.add(uri);
                                File compressedImageFile = null;
                                try {
                                    try {
                                        fileTemp = FileUtil.from(mContext, uri);
                                        Log.d("file", "File...:::: uti - " + fileTemp.getPath() + " file -" + fileTemp + " : " + fileTemp.exists());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    compressedImageFile = new Compressor(getContext()).compressToFile(fileTemp);
                                    filePart.add(MultipartBody.Part.createFormData("files", compressedImageFile.getName(),
                                            RequestBody.create(MediaType.parse("*/*"), compressedImageFile)));
                                    filesNames.add(new FileDataClass(compressedImageFile.getName(),common.getFolderSizeLabel(compressedImageFile),common.getMimeType(Uri.fromFile(compressedImageFile),mContext),uri.toString(),UUID.randomUUID().toString()));
                                    fileUri = uri.toString();
                                    fileType = common.getMimeType(Uri.fromFile(compressedImageFile),mContext);
                                    fileName = compressedImageFile.getName();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                if (Utils.isCheckFileSize(uri, mContext)) {
                                    try {
                                        fileTemp = FileUtil.from(mContext, uri);
                                        Log.d("file", "File...:::: uti - " + fileTemp.getPath() + " file -" + fileTemp + " : " + fileTemp.exists());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    filePart.add(MultipartBody.Part.createFormData("files", fileTemp.getName(),
                                            RequestBody.create(MediaType.parse("*/*"), fileTemp)));
                                    filesNames.add(new FileDataClass(fileTemp.getName(),common.getFolderSizeLabel(fileTemp),common.getMimeType(Uri.fromFile(fileTemp),getContext()),uri.toString(),UUID.randomUUID().toString()));
                                    fileUri = uri.toString();
                                    if (common.getMimeType(Uri.fromFile(fileTemp),mContext)!=null){
                                        fileType = common.getMimeType(Uri.fromFile(fileTemp),mContext);
                                    }else{
                                        String result = fileTemp.getAbsolutePath().substring(fileTemp.getAbsolutePath().lastIndexOf("."));
                                        String finalresult= result.replace(".", "");
                                        fileType = "application/"+finalresult;
                                   }
                                    fileName = fileTemp.getName();
                                } else {
                                    Toast.makeText(mContext, "File size exceeded from 2.5 mb", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                        if (!filePart.isEmpty()) {
                            Intent intent = new Intent(mContext, SelectFilePreviewActivity.class);
                            intent.putExtra("fileType",fileType);
                            intent.putExtra("fileUri",fileUri);
                            intent.putExtra("fileName",fileName);
                            intent.putExtra("fileSize",common.getFolderSizeLabel(fileTemp));
                            startActivity(intent);
                        }else{
                            Toast.makeText(mContext, "No file found", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            case 2:
                // Opening Camera
                if (requestCode == CAPTURE_PICTURE_FROM_CAMERA && resultCode == Activity.RESULT_OK) {
                    if(!mCurrentPhotoPath.isEmpty()) {
                        // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri;
                        if (Utils.isFileLessThan2MB(new File(mCurrentPhotoPath))){
                            contentUri = Uri.fromFile(new File(mCurrentPhotoPath));
                        }else{
                            contentUri = Uri.fromFile(common.compressImage(mCurrentPhotoPath,mContext));
                        }
                        mediaScanIntent.setData(contentUri);
                        if (mContext != null && isAdded()) {
                            mContext.sendBroadcast(mediaScanIntent);
                        }
                        File compressedImageFile = null;
                        try {
                            fileTemp = FileUtil.from(mContext, contentUri);
                            Log.d("file", "File...:::: uti - " + fileTemp.getPath() + " file -" + fileTemp + " : " + fileTemp.exists());
                            compressedImageFile = new Compressor(mContext).compressToFile(fileTemp);
                            filePart.add(MultipartBody.Part.createFormData("files", compressedImageFile.getName(),
                                    RequestBody.create(MediaType.parse("*/*"), compressedImageFile)));
                            filesNames.add(new FileDataClass(fileTemp.getName(),common.getFolderSizeLabel(compressedImageFile),common.getMimeType(Uri.fromFile(compressedImageFile),getContext()),contentUri.toString(),UUID.randomUUID().toString()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (!filePart.isEmpty()) {
                            //fragmentConversationsBinding.layoutSelectedFiles.setVisibility(View.VISIBLE);
                            Intent intent = new Intent(mContext, SelectFilePreviewActivity.class);
                            intent.putExtra("fileType",fileType);
                            intent.putExtra("fileUri",fileUri);
                            intent.putExtra("fileName",fileName);
                            intent.putExtra("fileSize",common.getFolderSizeLabel(compressedImageFile));
                            startActivity(intent);
                        }else{
                            Toast.makeText(mContext, "No file found", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            default:
                if(mContext!=null) {
                    //CreateDialoge.dialoge(mContext, "You haven't picked Image");
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewChatRecieveResponse event) {
        if(event!=null) {
            if (event.eventType.equalsIgnoreCase("CallSendPrivateAfterNewChat")) {
                tempChatId = event.conversation.tempChatId;
                conversationByUID = event.conversation.conversationUid;
                customerName = event.conversation.customerName;
                agentId = event.conversation.agentId;
                cusId = event.conversation.customerId;
                groupId = event.conversation.groupId;
                if (!uploadFilesData.isEmpty()){
                    sendMessage(event.conversation,"file",event.conversation.content,uploadFilesData);
                }else{
                    sendMessage(event.conversation,"text",event.conversation.content,uploadFilesData);
                    fragmentConversationsBinding.edtMessage.setText("");


                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ReLoadConversationEvent event) {
        if(event!=null) {
            if (event.eventType.equalsIgnoreCase("ReloadConversationWhenConnect")) {
                //api for all conversations
                //getConversationByUID(pageNumber,pageSize,conversationByUID,common.getUserId(mContext),false);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConversationEvent event) {
        if(event!=null){
            if(event.eventType.equalsIgnoreCase("AddToList")){
                ArrayList<Conversation> conversationArrayList = new ArrayList<>();
                if (common.getConversationList(mContext)!=null && !common.getConversationList(mContext).isEmpty()){
                    for (int i=0;i<common.getConversationList(mContext).size();i++){
                        if (!common.getConversationList(mContext).get(i).conversationUid.equalsIgnoreCase(conversationByUID)){
                            Conversation conversation = common.getConversationList(mContext).get(i);
                            conversation.isNewMessageReceive = true;
                            conversationArrayList.add(conversation);
                        }else{
                            conversationArrayList.add(common.getConversationList(mContext).get(i));
                        }
                    }
                    if(!conversationArrayList.isEmpty() && conversationsListingDetailAdapter!=null){
                        conversationsListingDetailAdapter = new ConversationsListingDetailAdapter(mContext,conversationArrayList);
                        fragmentConversationsBinding.rvConversationList.setAdapter(conversationsListingDetailAdapter);
                    }
                }
//
            }
        }

    }

}