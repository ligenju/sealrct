package com.xiangchuang.sealrtc;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import cn.rongcloud.rtc.base.RCRTCParamsType.AECMode;
import cn.rongcloud.rtc.base.RCRTCParamsType.NSLevel;
import cn.rongcloud.rtc.base.RCRTCParamsType.NSMode;
import cn.rongcloud.rtc.base.RCRTCParamsType.RCRTCVideoFps;
import cn.rongcloud.rtc.base.RCRTCParamsType.RCRTCVideoResolution;

import com.xiangchuang.sealrtc.base.RongRTCBaseActivity;
import com.xiangchuang.sealrtc.dialog.LoadDialog;
import com.xiangchuang.sealrtc.message.RoomInfoMessage;
import com.xiangchuang.sealrtc.utils.CustomizedEncryptionUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.rongcloud.rtc.api.RCRTCConfig;
import cn.rongcloud.rtc.api.RCRTCEngine;
import cn.rongcloud.rtc.api.RCRTCRoom;
import cn.rongcloud.rtc.api.callback.IRCRTCResultCallback;
import cn.rongcloud.rtc.api.callback.IRCRTCResultDataCallback;
import cn.rongcloud.rtc.api.stream.RCRTCAudioStreamConfig;
import cn.rongcloud.rtc.api.stream.RCRTCVideoStreamConfig;
import cn.rongcloud.rtc.base.RCRTCParamsType;
import cn.rongcloud.rtc.base.RCRTCRoomType;
import cn.rongcloud.rtc.base.RTCErrorCode;
import cn.rongcloud.rtc.utils.FinLog;
import io.rong.imlib.RongIMClient;

import static com.xiangchuang.sealrtc.utils.UserUtils.ACQUISITION_MODE_KEY;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_ENCRYPTION;
import static com.xiangchuang.sealrtc.utils.UserUtils.CAPTURE_CAMERA_DISPLAY_ORIENTATION_KEY;
import static com.xiangchuang.sealrtc.utils.UserUtils.CAPTURE_FRAME_ORIENTATION_KEY;
import static com.xiangchuang.sealrtc.utils.UserUtils.ENCODE_BIT_RATE_MODE_CQ;
import static com.xiangchuang.sealrtc.utils.UserUtils.ENCODE_BIT_RATE_MODE_VBR;
import static com.xiangchuang.sealrtc.utils.UserUtils.FPS;
import static com.xiangchuang.sealrtc.utils.UserUtils.IS_AUDIO_MUSIC;
import static com.xiangchuang.sealrtc.utils.UserUtils.IS_LIVE;
import static com.xiangchuang.sealrtc.utils.UserUtils.IS_MIRROR;
import static com.xiangchuang.sealrtc.utils.UserUtils.IS_STREAM_TINY;
import static com.xiangchuang.sealrtc.utils.UserUtils.IS_VIDEO_MUTE;
import static com.xiangchuang.sealrtc.utils.UserUtils.IS_OBSERVER;
import static com.xiangchuang.sealrtc.utils.UserUtils.KEY_USE_AV_SETTING;
import static com.xiangchuang.sealrtc.utils.UserUtils.RESOLUTION;
import static com.xiangchuang.sealrtc.utils.UserUtils.ROOMID;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_AGC_COMPRESSION;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_AGC_CONTROL_ENABLE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_AGC_LIMITER_ENABLE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_AGC_TARGET_DBOV;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_ECHO_CANCEL_FILTER_ENABLE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_ECHO_CANCEL_MODE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_NOISE_HIGH_PASS_FILTER;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_NOISE_SUPPRESSION_LEVEL;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_NOISE_SUPPRESSION_MODE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_PRE_AMPLIFIER_ENABLE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_PRE_AMPLIFIER_LEVEL;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_SAMPLE_RATE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_SOURCE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_STEREO_ENABLE;
import static com.xiangchuang.sealrtc.utils.UserUtils.AUDIO_TRANSPORT_BIT_RATE;
import static com.xiangchuang.sealrtc.utils.UserUtils.DECODER_COLOR_FORMAT_VAL_KEY;
import static com.xiangchuang.sealrtc.utils.UserUtils.DECODER_TYPE_KEY;
import static com.xiangchuang.sealrtc.utils.UserUtils.ENCODER_BIT_RATE_MODE;
import static com.xiangchuang.sealrtc.utils.UserUtils.ENCODER_COLOR_FORMAT_VAL_KEY;
import static com.xiangchuang.sealrtc.utils.UserUtils.ENCODER_LEVEL_KEY;
import static com.xiangchuang.sealrtc.utils.UserUtils.ENCODER_TYPE_KEY;
import static com.xiangchuang.sealrtc.utils.UserUtils.TOKEN;
import static com.xiangchuang.sealrtc.utils.UserUtils.VIDEO_ENCRYPTION;
import static com.xiangchuang.sealrtc.utils.UserUtils.IS_BENDI;

public class MainActivity extends RongRTCBaseActivity {
    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.CAMERA",
            "android.permission.READ_PHONE_STATE",
            "android.permission.FOREGROUND_SERVICE",
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH"
    };


    List<String> unGrantedPermissions;
    private static final String TAG = "MainActivity";
    private TextView tvStart, tvAdd;
    private Intent data = null;
    String userName = "李根聚";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        tvStart = findViewById(R.id.tv_start);
        tvAdd = findViewById(R.id.tv_add);
        tvStart.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                IS_BENDI = true;
                userName = "数字";
                TOKEN = "XrTYM5R34fQ67xZOZh3hFJAKNXH6t6upD8mOnYFfJ5g=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com";
                MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                startActivityForResult(manager.createScreenCaptureIntent(), 101);
            }
        });
        tvAdd.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                userName = "李根聚";
                TOKEN = "gsSBpAXZZ5OItvR8NSqhVwK82I/z0wqC//TahGlzUxE=@d00m.cn.rongnav.com;d00m.cn.rongcfg.com";
                MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                startActivityForResult(manager.createScreenCaptureIntent(), 101);
            }
        });

      /*  RongIMClient.setConnectionStatusListener(new RongIMClient.ConnectionStatusListener() {
            @Override
            public void onChanged(ConnectionStatus connectionStatus) {
                // 点击"开始会议"按钮时，IM为非CONNECTED时会主动connect，如果还是失败，自动化测试case失败
                // 监听IM连接状态，做1次自动加入房间的尝试，开发者可以忽略此修改
                if (ConnectionStatus.CONNECTED.equals(connectionStatus)) {
                    FinLog.d(TAG, "RongLog IM connected, Join Room");
                    connectToRoom();
                }
            }
        });*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent datas) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 101 || resultCode != Activity.RESULT_OK) {
            return;
        }
        data = datas;
        if (!TextUtils.isEmpty(TOKEN) && RongIMClient.getInstance().getCurrentConnectionStatus()
                == RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
            connectToRoom();
            return;
        }
        RongIMClient.connect(TOKEN, new RongIMClient.ConnectCallback() {


            /**
             * 成功回调
             * @param userIds 当前用户 ID
             */
            @Override
            public void onSuccess(String userIds) {
                connectToRoom();
                Log.d(TAG, "onSuccess: ");
            }

            /**
             * 错误回调
             * @param errorCode 错误码
             */
            @Override
            public void onError(RongIMClient.ConnectionErrorCode errorCode) {
                if (errorCode.equals(RongIMClient.ConnectionErrorCode.RC_CONN_TOKEN_INCORRECT)) {
                    //从 APP 服务获取新 token，并重连
                } else {
                    //无法连接 IM 服务器，请根据相应的错误码作出对应处理
                }
                if (errorCode == RongIMClient.ConnectionErrorCode.RC_CONNECTION_EXIST) {
                    connectToRoom();
                }
            }

            @Override
            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
                Log.d(TAG, "onDatabaseOpened: ");
            }
        });
    }

    private void connectToRoom() {
        if (RongIMClient.getInstance().getCurrentConnectionStatus()
                == RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
            LoadDialog.show(this);
            initOrUpdateRTCEngine();

            RCRTCRoomType roomType;
            if (IS_LIVE) {
                roomType = IS_VIDEO_MUTE ? RCRTCRoomType.LIVE_AUDIO : RCRTCRoomType.LIVE_AUDIO_VIDEO;
            } else {
                roomType = RCRTCRoomType.MEETING;
            }
            RCRTCEngine.getInstance().joinRoom(ROOMID, roomType, new IRCRTCResultDataCallback<RCRTCRoom>() {
                @Override
                public void onSuccess(RCRTCRoom room) {
                    LoadDialog.dismiss(MainActivity.this);
                    String toastMsg = getResources().getString(R.string.join_room_success);
                    Toast.makeText(MainActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    startCallActivity(IS_VIDEO_MUTE, IS_OBSERVER);
                    int userCount = room.getRemoteUsers().size();
                    Log.d(TAG, "onSuccess: 房间人数==" + userCount);
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                    LoadDialog.dismiss(MainActivity.this);
                    String toastMsg;
                    if (errorCode == RTCErrorCode.ServerUserBlocked) {
                        toastMsg = getResources().getString(R.string.rtc_dialog_forbidden_by_server);
                    } else {
                        toastMsg = getResources().getString(R.string.join_room_failed);
                    }
                    Toast.makeText(MainActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            String toastMsg = getResources().getString(R.string.im_connect_failed);
            Toast.makeText(MainActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void initOrUpdateRTCEngine() {
        RCRTCConfig.Builder configBuilder = RCRTCConfig.Builder.create();
        boolean audioEncryption = false;
        boolean videoEncryption = false;
        if (KEY_USE_AV_SETTING) {
            RCRTCParamsType.VideoBitrateMode videoBitrateMode;
            if (TextUtils.equals(ENCODER_BIT_RATE_MODE, ENCODE_BIT_RATE_MODE_CQ)) {
                videoBitrateMode = RCRTCParamsType.VideoBitrateMode.CQ;
            } else if (TextUtils.equals(ENCODER_BIT_RATE_MODE, ENCODE_BIT_RATE_MODE_VBR)) {
                videoBitrateMode = RCRTCParamsType.VideoBitrateMode.VBR;
            } else {
                videoBitrateMode = RCRTCParamsType.VideoBitrateMode.CBR;
            }
            /* 是否启动 AudioRecord */
            configBuilder.enableMicrophone(true)
                    /* 是否采用双声道 */
                    .enableStereo(AUDIO_STEREO_ENABLE)
                    /* 设置麦克采集来源 */
                    .setAudioSource(AUDIO_SOURCE)
                    /* 设置音频码率 */
                    .setAudioBitrate(AUDIO_TRANSPORT_BIT_RATE)
                    /* 设置音频采样率 */
                    .setAudioSampleRate(AUDIO_SAMPLE_RATE)
                    /* 视频编码配置 */
                    .enableHardwareEncoder(ENCODER_TYPE_KEY)
                    .setHardwareEncoderColor(ENCODER_COLOR_FORMAT_VAL_KEY)
                    .enableEncoderTexture(ACQUISITION_MODE_KEY)
                    .enableHardwareEncoderHighProfile(ENCODER_LEVEL_KEY)
                    /* 视频解码配置 */
                    .enableHardwareDecoder(DECODER_TYPE_KEY)
                    .setHardwareDecoderColor(DECODER_COLOR_FORMAT_VAL_KEY)
                    /* 编码码率控制模式 */
                    .setHardwareEncoderBitrateMode(videoBitrateMode)
                    /* 开启自定义音频加解密 */
                    .enableAudioEncryption(AUDIO_ENCRYPTION)
                    /* 开启自定义视频加解密 */.enableVideoEncryption(VIDEO_ENCRYPTION)
                    /* 开启SRTP*/.enableSRTP(false);
        }
        RCRTCEngine.getInstance().unInit();
        //自定义加解密 so 加载
        if (audioEncryption || videoEncryption) {
            CustomizedEncryptionUtil.getInstance().init();
        }
        Log.d(TAG, "initOrUpdateRTCEngine: ");
        FinLog.d(TAG + "", "init --> enter");
        String manufacturer = Build.MANUFACTURER.trim();
        if (manufacturer.contains("HUAWEI") || manufacturer.contains("vivo")) {
            configBuilder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            configBuilder.enableLowLatencyRecording(false);
        } else {
//            configBuilder.enableLowLatencyRecording(true);
        }
        configBuilder.enableLowLatencyRecording(true);
        RCRTCEngine.getInstance().init(getApplicationContext(), configBuilder.build());
        FinLog.d(TAG + "", "init --> over");

        RCRTCAudioStreamConfig.Builder audioConfigBuilder = RCRTCAudioStreamConfig.Builder.create();
        if (KEY_USE_AV_SETTING) {
            /* Audio Echo Cancel */
            audioConfigBuilder.setEchoCancel(AECMode.parseValue(AUDIO_ECHO_CANCEL_MODE))
                    .enableEchoFilter(AUDIO_ECHO_CANCEL_FILTER_ENABLE)
                    /* Audio Noise Suppression */
                    .setNoiseSuppression(NSMode.parseValue(AUDIO_NOISE_SUPPRESSION_MODE))
                    .setNoiseSuppressionLevel(NSLevel.parseValue(AUDIO_NOISE_SUPPRESSION_LEVEL))
                    .enableHighPassFilter(AUDIO_NOISE_HIGH_PASS_FILTER)
                    /* Audio AGC Config */
                    .enableAGCControl(AUDIO_AGC_CONTROL_ENABLE)
                    .enableAGCLimiter(AUDIO_AGC_LIMITER_ENABLE)
                    .setAGCTargetdbov(AUDIO_AGC_TARGET_DBOV)
                    .setAGCCompression(AUDIO_AGC_COMPRESSION)
                    .enablePreAmplifier(AUDIO_PRE_AMPLIFIER_ENABLE)
                    .setPreAmplifierLevel(AUDIO_PRE_AMPLIFIER_LEVEL);
            RCRTCAudioStreamConfig audioStreamConfig = IS_AUDIO_MUSIC ?
                    audioConfigBuilder.buildMusicMode() : audioConfigBuilder.buildDefaultMode();
            FinLog.d(TAG + "", "Audio --> enter");
            RCRTCEngine.getInstance().getDefaultAudioStream().setAudioConfig(audioStreamConfig);
        }

        RCRTCVideoStreamConfig.Builder videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create();
        // 如果开启了镜像翻转 VideoFrame，则应关闭镜像预览功能，否则会造成2次翻转效果
        RCRTCEngine.getInstance().getDefaultVideoStream().setPreviewMirror(!IS_MIRROR);
        /* 视频分辨率/码率 */
//        String maxBitRate = sm.getString(SettingActivity.BIT_RATE_MAX, "");
//        String minBitRate = sm.getString(SettingActivity.BIT_RATE_MIN);
//        if (!TextUtils.isEmpty(maxBitRate)) {
//            videoConfigBuilder.setMaxRate(Integer.parseInt(maxBitRate.substring(0, maxBitRate.length() - 4)));
//        }
//        if (!TextUtils.isEmpty(minBitRate)) {
//            videoConfigBuilder.setMinRate(Integer.parseInt(minBitRate.substring(0, minBitRate.length() - 4)));
//        }
        videoConfigBuilder.setVideoResolution(selectiveResolution(RESOLUTION))
                .setVideoFps(selectiveFrame(FPS));
        RCRTCEngine.getInstance().getDefaultVideoStream().
                enableTinyStream(IS_STREAM_TINY);
        RCRTCEngine.getInstance().getDefaultVideoStream().setCameraDisplayOrientation(
                CAPTURE_CAMERA_DISPLAY_ORIENTATION_KEY);
        RCRTCEngine.getInstance().getDefaultVideoStream().setFrameOrientation(
                CAPTURE_FRAME_ORIENTATION_KEY);
        RCRTCEngine.getInstance().getDefaultVideoStream().setVideoConfig(videoConfigBuilder.build());
    }

    /**
     * 构造分辨率对应的BlinkVideoProfile对象
     *
     * @param resolutionStr
     * @return
     */
    private RCRTCVideoResolution selectiveResolution(String resolutionStr) {
        RCRTCVideoResolution profile = null;
        if (resolutionStr == null || resolutionStr.equals("")) {
            return RCRTCVideoResolution.RESOLUTION_480_640;
        }
        String[] resolutionArray = resolutionStr.split("x");
        profile = RCRTCVideoResolution.parseVideoResolution(
                Integer.parseInt(resolutionArray[0]), Integer.parseInt(resolutionArray[1]));
        return profile;
    }

    private RCRTCVideoFps selectiveFrame(String frameStr) {
        frameStr = TextUtils.isEmpty(frameStr) ? "15" : frameStr;
        return RCRTCVideoFps.parseVideoFps(Integer.parseInt(frameStr));
    }

    private void startCallActivity(boolean muteVideo, boolean observer) {
        Intent intent = null;
//        使用会议界面进行音视频
        intent = new Intent(this, CallActivity.class);
        intent.putExtra(CallActivity.EXTRA_USER_NAME, userName);
        RCRTCRoom room = RCRTCEngine.getInstance().getRoom();
        int joinMode = RoomInfoMessage.JoinMode.AUDIO_VIDEO;
        if (muteVideo) {
            joinMode = RoomInfoMessage.JoinMode.AUDIO;
        }
        if (observer) {
            joinMode = RoomInfoMessage.JoinMode.OBSERVER;
        }
        String userId = room.getLocalUser().getUserId();
        int remoteUserCount = room.getRemoteUsers() != null ? room.getRemoteUsers().size() : 0;
        intent.putExtra(CallActivity.EXTRA_IS_MASTER, remoteUserCount == 0);
        intent.putExtra("DATA", data);
        RoomInfoMessage roomInfoMessage = new RoomInfoMessage(
                userId, userName, joinMode, System.currentTimeMillis(), remoteUserCount == 0);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userId", userId);
            jsonObject.put("userName", userName);
            jsonObject.put("joinMode", joinMode);
            jsonObject.put("joinTime", System.currentTimeMillis());
            jsonObject.put("master", remoteUserCount == 0 ? 1 : 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        room.setRoomAttribute(userId, jsonObject.toString(), roomInfoMessage, new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {

            }
        });
        startActivity(intent);
    }

    private void checkPermissions() {
        unGrantedPermissions = new ArrayList();
        for (String permission : MANDATORY_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        }
        if (unGrantedPermissions.size() > 0) {
            String[] array = new String[unGrantedPermissions.size()];
            ActivityCompat.requestPermissions(this, unGrantedPermissions.toArray(array), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        unGrantedPermissions.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                unGrantedPermissions.add(permissions[i]);
        }
        if (unGrantedPermissions.size() > 0) {
            for (String permission : unGrantedPermissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    Toast.makeText(
                            this,
                            "权限:"
                                    + permission
                                    + "已被禁止，请手动开启！",
                            Toast.LENGTH_SHORT)
                            .show();
                    finish();
                } else ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LoadDialog.dismiss(MainActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        RongIMClient.setConnectionStatusListener(null);
    }
}