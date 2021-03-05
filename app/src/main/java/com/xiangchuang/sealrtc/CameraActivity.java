package com.xiangchuang.sealrtc;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.my.mylibrary.RongRTC;
import com.my.mylibrary.dialog.PromptDialog;

import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity {

    private View v_tip;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        RongRTC.newInstance().addActivity(this);
        initView();
    }
    private PromptDialog promptDialog;
    private PromptDialog quitDialog;
    boolean isNoDialog = true;
    private static final String TAG = "CameraActivity";


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "onWindowFocusChanged: 状态栏=" + hasFocus);
        try {
            if (isNoDialog && !hasFocus && (quitDialog == null || quitDialog != null && !quitDialog.isShowing())) {
                v_tip.setVisibility(View.VISIBLE);
                RongRTC.newInstance().cancelScreenCast();
                promptDialog = PromptDialog.newInstance(CameraActivity.this, "提示", "服务已断开确定继续直播吗？", "继续直播", "退出");
                promptDialog.setPromptButtonClickedListener(new PromptDialog.OnPromptButtonClickedListener() {
                    @Override
                    public void onPositiveButtonClicked() {
                        isNoDialog = false;
                        RongRTC.newInstance().getMediaProjectionService(CameraActivity.this);
                    }

                    @Override
                    public void onNegativeButtonClicked() {
                        RongRTC.newInstance().intendToLeave(true, "");
                    }
                });
                promptDialog.setCancelable(false);
                promptDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isNoDialog = true;
        if (!RongRTC.newInstance().isBy(requestCode, resultCode)) {
            promptDialog = PromptDialog.newInstance(CameraActivity.this, "提示", "服务已断开确定继续直播吗？", "继续直播", "退出");
            promptDialog.setPromptButtonClickedListener(new PromptDialog.OnPromptButtonClickedListener() {
                @Override
                public void onPositiveButtonClicked() {
                    isNoDialog = false;
                    RongRTC.newInstance().getMediaProjectionService(CameraActivity.this);
                }

                @Override
                public void onNegativeButtonClicked() {
                    RongRTC.newInstance().intendToLeave(true, "");
                }
            });
            promptDialog.setCancelable(false);
            promptDialog.show();
            return;
        }
        v_tip.setVisibility(View.GONE);
        RongRTC.newInstance().postSharedVideo(data);
    }
    @Override
    public void onBackPressed() {
        withdraw();
    }

    @OnClick(R.id.call_btn_hangup)
    public void onViewClicked() {
        withdraw();
    }

    public void withdraw() {
        quitDialog = PromptDialog.newInstance(CameraActivity.this, "提示", "确定退出吗？");
        quitDialog.setPromptButtonClickedListener(new PromptDialog.OnPromptButtonClickedListener() {
            @Override
            public void onPositiveButtonClicked() {
                RongRTC.newInstance().intendToLeave(true, "");
            }

            @Override
            public void onNegativeButtonClicked() {
            }
        });
        quitDialog.setCancelable(false);
        quitDialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initView() {
        FrameLayout  containerCamera = (FrameLayout) findViewById(R.id.container_camera);
        CameraUtils.init(this);
        PhotoFragment photoFragment = new PhotoFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_camera, photoFragment)
                .commit();
        v_tip = (View) findViewById(R.id.v_tip);
        findViewById(R.id.call_btn_hangup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RongRTC.newInstance().intendToLeave(true, "");
            }
        });
    }
}