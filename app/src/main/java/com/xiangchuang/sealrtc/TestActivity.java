package com.xiangchuang.sealrtc;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.my.mylibrary.RongRTC;
import com.my.mylibrary.utils.UserUtils;


public class TestActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.lin_ok).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                RongRTC.newInstance().getMediaProjectionService(TestActivity.this);
            }
        });
        RongRTC.newInstance().setOnRongYunConnectionMonitoring(new RongRTC.OnRongYunConnectionMonitoring() {
            @Override
            public void onConnectionSucceeded(boolean isAdmin) {
                startActivity(new Intent(TestActivity.this, CameraActivity.class));
            }

            @Override
            public void onConnectionFailure(String err) {
                Toast.makeText(TestActivity.this, err, Toast.LENGTH_SHORT).show();
            }
        });




//        findViewById(R.id.tv_start).setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//            @Override
//            public void onClick(View v) {
////                "XrTYM5R34fQ67xZOZh3hFJAKNXH6t6upD8mOnYFfJ5g=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com";
//                RongRTC.newInstance().start(TestActivity.this, null, "gsSBpAXZZ5OItvR8NSqhVwK82I/z0wqC//TahGlzUxE=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com", "123456", "夫子", false);
//            }
//        });
////        RongRTC.newInstance().setOnRongYunConnectionMonitoring(new RongRTC.OnRongYunConnectionMonitoring() {
////            @Override
////            public void onConnectionSucceeded(boolean isAdmin) {
//////                Intent intent = new Intent(TestActivity.this, MeetingActivity.class);
//////                intent.putExtra(MeetingActivity.EXTRA_IS_MASTER, isAdmin);
//////                startActivity(intent);
////            }
////
////            @Override
////            public void onConnectionFailure(String err) {
////                Toast.makeText(TestActivity.this, "加入房间失败==" + err, Toast.LENGTH_SHORT).show();
////            }
////        });
//        findViewById(R.id.tv_add).setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//            @Override
//            public void onClick(View v) {
//                RongRTC.newInstance().start(TestActivity.this, null, "XrTYM5R34fQ67xZOZh3hFJAKNXH6t6upD8mOnYFfJ5g=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com","123456", "观看者", false);
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!RongRTC.newInstance().isBy(requestCode, resultCode)) {
            return;
        }
        UserUtils.IS_BENDI = true;
        RongRTC.newInstance().start(this, data, "RVHMoPiaLKvZcRsdkrdfAVjcrJI7YoQZsqcINLIIaFE=@aqq0.cn.rongnav.com;aqq0.cn.rongcfg.com", "123456", "播放者", true);
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