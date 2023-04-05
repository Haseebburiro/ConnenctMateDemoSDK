package com.chat.befiler.activities;

import static com.chat.befiler.commons.Constants.HIDE_TOOLBAR;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import com.bumptech.glide.Glide;
import com.chat.befiler.Events.appEvents.MessageEvent;
import com.chat.befiler.Events.chatEvents.SendFileAfterPreview;
import com.chat.befiler.commons.Common;
import com.chat.befiler.fragments.ConversationsDetailFragment;
import com.chat.befiler.model.chat.NewChatModel;
import com.example.signalrtestandroid.R;
import com.example.signalrtestandroid.databinding.ActivitySplashBinding;
import com.example.signalrtestandroid.databinding.FragmentFilePreviewBinding;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.vanniktech.emoji.EmojiPopup;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import org.greenrobot.eventbus.EventBus;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;

public class SelectFilePreviewActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener,
        OnPageErrorListener {

    private FragmentFilePreviewBinding binding;
    Intent intent1;
    String fileType = "";
    String fileUri = "";
    String fileName = "";
    String fileSize = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = FragmentFilePreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final EmojiPopup popup = EmojiPopup.Builder
                .fromRootView(binding.getRoot()).build(binding.edtMessage);
        if (getIntent()!=null){
            intent1 = getIntent();
            if(intent1.getExtras().containsKey("fileType") && intent1.getExtras().containsKey("fileUri")&& intent1.getExtras().containsKey("fileName") && intent1.getExtras().containsKey("fileSize")){
                if(!intent1.getExtras().getString("fileType").isEmpty() && !intent1.getExtras().getString("fileUri").isEmpty()&& !intent1.getExtras().getString("fileName").isEmpty()&& !intent1.getExtras().getString("fileSize").isEmpty()){
                    fileType = intent1.getExtras().getString("fileType");
                    fileUri = intent1.getExtras().getString("fileUri");
                    fileName = intent1.getExtras().getString("fileName");
                    fileSize = intent1.getExtras().getString("fileSize");

                    if(fileType.equalsIgnoreCase("image/jpeg") || fileType.equalsIgnoreCase("image/png") || fileType.equalsIgnoreCase("image/jpg")){
                        binding.pdfView.setVisibility(View.GONE);
                        binding.selectedImageView.setVisibility(View.VISIBLE);
                        binding.layoutOtherFiles.setVisibility(View.GONE);

                        Uri myUri = Uri.parse(fileUri);
                        binding.selectedImageView.setImageURI(myUri);
                    }
                    else if (fileType.equalsIgnoreCase("application/msword") ||fileType.equalsIgnoreCase("docx") || fileType.equalsIgnoreCase("application/docx") || fileType.equalsIgnoreCase("doc") || fileType.equalsIgnoreCase("application/doc")|| fileType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")){
                        binding.pdfView.setVisibility(View.GONE);
                        binding.selectedImageView.setVisibility(View.GONE);
                        binding.layoutOtherFiles.setVisibility(View.VISIBLE);
                        binding.ivFileImage.setImageResource(R.drawable.doc);
                        binding.txtFileName.setText(fileName);
                        binding.txtSize.setText(fileSize);

                    }
                    else if(fileType.equalsIgnoreCase("application/pdf")){
                        binding.layoutOtherFiles.setVisibility(View.GONE);
                        binding.selectedImageView.setVisibility(View.GONE);
                        binding.pdfView.setVisibility(View.VISIBLE);
                        binding.pdfView.fromUri(Uri.parse(fileUri))
                                .defaultPage(0)
                                .onPageChange(this)
                                .enableAnnotationRendering(true)
                                .onLoad(this)
                                .scrollHandle(new DefaultScrollHandle(this))
                                .spacing(10) // in dp
                                .onPageError(this)
                                .load();
                    }
                    else{
                        binding.pdfView.setVisibility(View.GONE);
                        binding.selectedImageView.setVisibility(View.GONE);
                        binding.layoutOtherFiles.setVisibility(View.VISIBLE);
                        binding.txtFileName.setText(fileName);
                        binding.txtSize.setText(fileSize);
                            if (fileType.equalsIgnoreCase("zip") || fileType.equalsIgnoreCase("application/zip")){
                                binding.ivFileImage.setImageResource(R.drawable.zip);
                            }
                            else if (fileType.equalsIgnoreCase("rar") || fileType.equalsIgnoreCase("application/rar")){
                                binding.ivFileImage.setImageResource(R.drawable.rar);
                            }
                            else if (fileType.equalsIgnoreCase("7z") || fileType.equalsIgnoreCase("application/7z")){
                                binding.ivFileImage.setImageResource(R.drawable.sevenz);
                            }
                            else if (fileType.equalsIgnoreCase("txt") || fileType.equalsIgnoreCase("application/txt")){
                                binding.ivFileImage.setImageResource(R.drawable.txt);
                            }

                            else if (fileType.equalsIgnoreCase("xls") || fileType.equalsIgnoreCase("application/xls")){
                                binding.ivFileImage.setImageResource(R.drawable.xls);
                            }
                            else if (fileType.equalsIgnoreCase("csv") || fileType.equalsIgnoreCase("application/csv")){
                                binding.ivFileImage.setImageResource(R.drawable.xls);
                            }
                    }
                }
            }
        }


        binding.ivSmile.setOnClickListener(v -> {
            binding.ivSmile.setVisibility(View.GONE);
            binding.ivKeyboard.setVisibility(View.VISIBLE);
            popup.toggle();
        });
        binding.ivKeyboard.setOnClickListener(v -> {
            binding.ivKeyboard.setVisibility(View.GONE);
            binding.ivSmile.setVisibility(View.VISIBLE);
            popup.dismiss();
        });

        KeyboardVisibilityEvent.setEventListener(SelectFilePreviewActivity.this, isOpen -> {
            if (isOpen){
                binding.ivSmile.setVisibility(View.GONE);
                binding.ivKeyboard.setVisibility(View.VISIBLE);
            } else{
                binding.ivSmile.setVisibility(View.VISIBLE);
                binding.ivKeyboard.setVisibility(View.GONE);
            }

        });
        binding.menuClick.setOnClickListener(v -> {
            ConversationsDetailFragment.isCalledFromPreviewActivity = false;
            finish();
        });

        binding.sendMessage.setOnClickListener(v -> {
            String message = "";
            if (binding.edtMessage.getText().toString().isEmpty()){
                message = fileName;
            }else{
                message = binding.edtMessage.getText().toString();
            }
            ConversationsDetailFragment.isCalledFromPreviewActivity = true;
            SendFileAfterPreview sendFileAfterPreview =new SendFileAfterPreview(message,fileUri,fileType,fileName,"SendFileAfterPreview");
            ConversationsDetailFragment.sendFileAfterPreview = sendFileAfterPreview;
            finish();

        });

    }

    @Override
    public void onPageChanged(int page, int pageCount) {

    }

    @Override
    public void loadComplete(int nbPages) {

    }

    @Override
    public void onPageError(int page, Throwable t) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}