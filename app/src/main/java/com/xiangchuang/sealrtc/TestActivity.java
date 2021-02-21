package com.xiangchuang.sealrtc;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.my.mylibrary.RongRTC;
import com.my.mylibrary.utils.UserUtils;


public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.tv_test).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                RongRTC.newInstance().getMediaProjectionService(TestActivity.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!RongRTC.newInstance().isBy(requestCode, resultCode)) {
            return;
        }
        UserUtils.IS_BENDI = true;
        RongRTC.newInstance().start(this, data, UserUtils.TOKEN, UserUtils.ROOMID);
    }

    @Override
    protected void onStop() {
        super.onStop();
        RongRTC.newInstance().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RongRTC.newInstance().onDestroy();
    }
}