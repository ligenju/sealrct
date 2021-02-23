package com.xiangchuang.sealrtc;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.my.mylibrary.RongRTC;

public class CameraActivity extends AppCompatActivity {

    private FrameLayout containerCamera;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initView();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initView() {
        containerCamera = (FrameLayout) findViewById(R.id.container_camera);
        CameraUtils.init(this);
        PhotoFragment photoFragment = new PhotoFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_camera, photoFragment)
                .commit();
        findViewById(R.id.call_btn_hangup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RongRTC.newInstance().intendToLeave(true);
                finish();
            }
        });
    }
}