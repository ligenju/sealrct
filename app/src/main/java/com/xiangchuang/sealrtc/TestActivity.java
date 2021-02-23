package com.xiangchuang.sealrtc;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.my.mylibrary.RongRTC;
import com.my.mylibrary.utils.UserUtils;

import io.rong.imlib.RongIMClient;


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
        findViewById(R.id.tv_start).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
//                "XrTYM5R34fQ67xZOZh3hFJAKNXH6t6upD8mOnYFfJ5g=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com";
                RongRTC.newInstance().start(TestActivity.this, null, "gsSBpAXZZ5OItvR8NSqhVwK82I/z0wqC//TahGlzUxE=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com", "123456", "夫子", false);
            }
        });
        RongRTC.newInstance().setOnRongYunConnectionMonitoring(new RongRTC.OnRongYunConnectionMonitoring() {
            @Override
            public void onConnectionSucceeded(boolean isAdmin) {
//                Intent intent = new Intent(TestActivity.this, MeetingActivity.class);
//                intent.putExtra(MeetingActivity.EXTRA_IS_MASTER, isAdmin);
//                startActivity(intent);
            }

            @Override
            public void onConnectionFailure(String err) {
                Toast.makeText(TestActivity.this, "加入房间失败==" + err, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.tv_add).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                RongRTC.newInstance().start(TestActivity.this, null, "XrTYM5R34fQ67xZOZh3hFJAKNXH6t6upD8mOnYFfJ5g=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com","123456", "观看者", false);
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
        RongRTC.newInstance().start(this, data, "gsSBpAXZZ5OItvR8NSqhVwK82I/z0wqC//TahGlzUxE=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com", "123456", "播放者", true);
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