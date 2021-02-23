package com.xiangchuang.sealrtc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.my.mylibrary.RongRTC;
import com.my.mylibrary.utils.UserUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class TestActivity extends AppCompatActivity {
    List<String> unGrantedPermissions;
    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.CAMERA",
            "android.permission.READ_PHONE_STATE",
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH"
    };
    private int type = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        findViewById(R.id.lin_ok).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                type = 1;
                checkPermissions();
            }
        });
        findViewById(R.id.lin_add).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                type = 0;
                checkPermissions();
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void checkPermissions() {
        unGrantedPermissions = new ArrayList();
        for (String permission : MANDATORY_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        }
        if (unGrantedPermissions.size() == 0) { // 已经获得了所有权限，开始加入聊天室
            if (type == 1)
                RongRTC.newInstance().getMediaProjectionService(TestActivity.this);
            else
                RongRTC.newInstance().start(TestActivity.this, null, "RVHMoPiaLKvNUA+voneA9FjcrJI7YoQZnUY8m94JVxc=@aqq0.cn.rongnav.com;aqq0.cn.rongcfg.com", UserUtils.ROOMID, "查看者", false);
        } else { // 部分权限未获得，重新请求获取权限
            String[] array = new String[unGrantedPermissions.size()];
            ActivityCompat.requestPermissions(this, unGrantedPermissions.toArray(array), 0);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        unGrantedPermissions.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                unGrantedPermissions.add(permissions[i]);
        }
        for (String permission : unGrantedPermissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Toast.makeText(this, getString(R.string.PermissionStr) + permission + getString(R.string.plsopenit), Toast.LENGTH_SHORT).show();
            } else ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        }
        if (unGrantedPermissions.size() == 0) {
            RongRTC.newInstance().getMediaProjectionService(TestActivity.this);
        }
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