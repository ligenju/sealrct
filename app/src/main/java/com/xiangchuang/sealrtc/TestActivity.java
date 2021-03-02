package com.xiangchuang.sealrtc;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.my.mylibrary.OnRongYunConnectionMonitoring;
import com.my.mylibrary.RongRTC;
import com.my.mylibrary.utils.UserUtils;

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
    private static final String TAG = "TestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        RongRTC.newInstance().initConnectionStatusListener();
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
        RongRTC.newInstance().setOnRongYunConnectionMonitoring(new OnRongYunConnectionMonitoring() {
            @Override
            public void onTokenFail() {

            }

            @Override
            public void onConnectionRongYunFailed(String err) {
                showToast(err);
            }

            @Override
            public void onConnectedToTheRoomSuccessfully(boolean isShare, boolean isAdmin) {
                if (isShare) {
                    startActivity(new Intent(TestActivity.this, CameraActivity.class));
                } else {
                    Intent intent = new Intent(TestActivity.this, RongYunSeeActivity.class);
                    intent.putExtra("EXTRA_IS_MASTER", isAdmin);
                    startActivity(intent);
//                    RongRTC.newInstance().startCallActivity(isAdmin);
                }
            }

            @Override
            public void onFailedToConnectToRoom(String err) {
                showToast(err);
            }

            @Override
            public void onFailedToShareScreen(String err) {
                showToast(err);
            }

            @Override
            public void onSuccessfullySubscribed() {
//                showToast("成功订阅");
                Log.d(TAG, "成功订阅");
            }

            @Override
            public void onFailedSubscription(String err) {
                showToast(err);
            }

            @Override
            public void onUserOffline(String name) {
                showToast(name + "   离线");
            }

            @Override
            public void onUserLeft(String name) {
                showToast(name + "  退出房间");
            }

            @Override
            public void onSuccessfullyExitTheRoom(String prompt) {
                showToast(TextUtils.isEmpty(prompt) ? "退出成功" : prompt);
            }

            @Override
            public void onReceiveMessage(String message) {
                showToast(message);
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onDestroyed(String reason) {
                if (!UserUtils.IS_BENDI) {
                    List<Activity> list = RongRTC.newInstance().getActivityList();
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i) instanceof RongYunSeeActivity) {
                            RongYunSeeActivity activity = (RongYunSeeActivity) list.get(i);
                            activity.intendToLeave(true, "");
                        }
                    }
                } else {
                    RongRTC.newInstance().intendToLeave(true, reason);
                }
            }
        });

    }

    private void showToast(String err) {
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
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
                RongRTC.newInstance().start(TestActivity.this, null, "RVHMoPiaLKvNUA+voneA9FjcrJI7YoQZnUY8m94JVxc=@aqq0.cn.rongnav.com;aqq0.cn.rongcfg.com", UserUtils.ROOMID, "查看者");
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
            if (type == 1)
                RongRTC.newInstance().getMediaProjectionService(TestActivity.this);
            else
                RongRTC.newInstance().start(TestActivity.this, null, "RVHMoPiaLKvNUA+voneA9FjcrJI7YoQZnUY8m94JVxc=@aqq0.cn.rongnav.com;aqq0.cn.rongcfg.com", UserUtils.ROOMID, "查看者");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!RongRTC.newInstance().isBy(requestCode, resultCode)) {
            return;
        }
        RongRTC.newInstance().start(this, data, "RVHMoPiaLKvZcRsdkrdfAVjcrJI7YoQZsqcINLIIaFE=@aqq0.cn.rongnav.com;aqq0.cn.rongcfg.com", "123456", "播放者");
    }

    @Override
    protected void onDestroy() {
        RongRTC.newInstance().onDestroy();
        super.onDestroy();
    }

}