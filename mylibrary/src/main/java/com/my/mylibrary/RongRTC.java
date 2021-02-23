package com.my.mylibrary;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.my.mylibrary.bean.ItemModel;
import com.my.mylibrary.bean.UserInfo;
import com.my.mylibrary.broadcast.HomeWatcherReceiver;
import com.my.mylibrary.call.AppRTCAudioManager;
import com.my.mylibrary.dialog.PromptDialog;
import com.my.mylibrary.message.RoomInfoMessage;
import com.my.mylibrary.message.RoomKickOffMessage;
import com.my.mylibrary.screen_cast.RongRTCScreenCastHelper;
import com.my.mylibrary.utils.BluetoothUtil;
import com.my.mylibrary.utils.CustomizedEncryptionUtil;
import com.my.mylibrary.utils.HeadsetPlugReceiver;
import com.my.mylibrary.utils.OnHeadsetPlugListener;
import com.my.mylibrary.utils.SessionManager;
import com.my.mylibrary.utils.UserUtils;
import com.my.mylibrary.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.rongcloud.rtc.api.RCRTCAudioMixer;
import cn.rongcloud.rtc.api.RCRTCConfig;
import cn.rongcloud.rtc.api.RCRTCEngine;
import cn.rongcloud.rtc.api.RCRTCLocalUser;
import cn.rongcloud.rtc.api.RCRTCRemoteUser;
import cn.rongcloud.rtc.api.RCRTCRoom;
import cn.rongcloud.rtc.api.callback.IRCRTCAudioDataListener;
import cn.rongcloud.rtc.api.callback.IRCRTCResultCallback;
import cn.rongcloud.rtc.api.callback.IRCRTCResultDataCallback;
import cn.rongcloud.rtc.api.callback.IRCRTCRoomEventsListener;
import cn.rongcloud.rtc.api.stream.RCRTCAudioStreamConfig;
import cn.rongcloud.rtc.api.stream.RCRTCCameraOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCInputStream;
import cn.rongcloud.rtc.api.stream.RCRTCMicOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoStreamConfig;
import cn.rongcloud.rtc.api.stream.RCRTCVideoView;
import cn.rongcloud.rtc.base.RCRTCAudioFrame;
import cn.rongcloud.rtc.base.RCRTCMediaType;
import cn.rongcloud.rtc.base.RCRTCParamsType;
import cn.rongcloud.rtc.base.RCRTCRoomType;
import cn.rongcloud.rtc.base.RTCErrorCode;
import cn.rongcloud.rtc.utils.FinLog;
import io.rong.calllib.RongCallClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.MessageContent;


/**
 * Description:
 * <p>
 * CreateTime：2021/02/20  17:03
 */
public class RongRTC {
    //房间
    private RCRTCRoom room;
    private RCRTCLocalUser localUser;
    //是否已连接
    private boolean isConnected = true;
    //共享屏幕判断只能一个人共享  true=无人；false=有人
    private boolean screenCastEnable = true;
    private boolean kicked = false;
    //屏幕投射助手
    private RongRTCScreenCastHelper screenCastHelper;
    //共享屏幕的流
    private RCRTCVideoOutputStream screenOutputStream;
    //是否是你的房间
    private boolean isInRoom;
    private List<ItemModel> mMembers = new ArrayList<>();
    private Map<String, UserInfo> mMembersMap = new HashMap<>();
    //我的userID；
    private String myUserId;
    private String adminUserId;
//    private boolean admin;
    /**
     * true 关闭麦克风,false 打开麦克风
     */
    private boolean muteMicrophone = false;
    /**
     * true 关闭扬声器； false 打开扬声器
     */
    private boolean muteSpeaker = false;
    //音频
    private AppRTCAudioManager audioManager = null;
    //监听耳机
    private HeadsetPlugReceiver headsetPlugReceiver = null;
    public static Intent data;

    public static int MEDIA_PROJECTION_SERVICE_CODE = 101;

    public static String TAG = "RongRTC>>>>>>";
    private static RongRTC rongRTC;

    private RongRTC() {
    }

    public static synchronized RongRTC newInstance() {
        if (rongRTC == null)
            rongRTC = new RongRTC();
        return rongRTC;
    }

    /**
     * 融云初始化 在application里
     *
     * @param context
     * @param appKey  融云appKey
     */
    public static void init(Context context, String appKey) {
        Utils.init(context);
        SessionManager.initContext(context);
        RongIMClient.init(context, appKey, false);
        if (context.getApplicationInfo().packageName.equals(Utils.getCurProcessName(context))) {
            try {
                RongIMClient.registerMessageType(RoomInfoMessage.class);
                RongIMClient.registerMessageType(RoomKickOffMessage.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取共享屏幕权限服务
     *
     * @param activity
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void getMediaProjectionService(Activity activity) {
        MediaProjectionManager manager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(manager.createScreenCaptureIntent(), MEDIA_PROJECTION_SERVICE_CODE);
    }

    /**
     * 是否获取到共享屏幕权限服务
     *
     * @param requestCode
     * @param resultCode
     * @return
     */
    public boolean isBy(int requestCode, int resultCode) {
        if (requestCode != 101 || resultCode != Activity.RESULT_OK) {
            return false;
        }
        return true;
    }

    public boolean isConnected() {
        if (!TextUtils.isEmpty(UserUtils.TOKEN) && RongIMClient.getInstance().getCurrentConnectionStatus()
                == RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)
            return true;

        return false;
    }

    public void start(Activity activity, Intent datas, String token, String roomId, String username, boolean isShared) {
        HomeWatcherReceiver.registerHomeKeyReceiver(activity);
        data = datas;
        UserUtils.activity = activity;
        UserUtils.ROOMID = roomId;
        UserUtils.TOKEN = token;
        UserUtils.USER_NAME = username;
        UserUtils.IS_BENDI = isShared;

        if (isShared) {
            UserUtils.CONNECTION_STATUS = 1;
        } else {
            UserUtils.CONNECTION_STATUS = 2;
        }
        if (!TextUtils.isEmpty(UserUtils.TOKEN) && RongIMClient.getInstance().getCurrentConnectionStatus()
                == RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
            connectToRoom();
            return;
        }
        RongIMClient.connect(UserUtils.TOKEN, new RongIMClient.ConnectCallback() {


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

    /**
     * 准备离开当前房间
     *
     * @param initiative 是否主动退出，false为被踢的情况
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void intendToLeave(boolean initiative) {
        FinLog.i(TAG, "intendToLeave()-> " + initiative);
        HomeWatcherReceiver.unregisterHomeKeyReceiver(UserUtils.activity);
        cancelScreenCast(true);
        if (initiative) {
            selectAdmin();
        } else {
            kicked = true;
        }
        RCRTCAudioMixer.getInstance().stop();
        // 当前用户是观察者 或 离开房间时还有其他用户存在，直接退出
        if (screenOutputStream == null || UserUtils.IS_BENDI) {
            disconnect();
        }
    }

    /**
     * 停止方法
     */
    public void onStop() {
        if (isInRoom) {
            RCRTCEngine.getInstance().getDefaultVideoStream().stopCamera();
        }
    }

    public void onDestroy() {
        HeadsetPlugReceiver.setOnHeadsetPlugListener(null);
        if (headsetPlugReceiver != null) {
            UserUtils.activity.unregisterReceiver(headsetPlugReceiver);
            headsetPlugReceiver = null;
        }
        HeadsetPlugReceiverState = false;
        if (room != null) {
            room.unregisterRoomListener();
        }
        RCRTCEngine.getInstance().unregisterStatusReportListener();
        if (isConnected) {
            RCRTCAudioMixer.getInstance().stop();
            if (room != null) {
                room.deleteRoomAttributes(Arrays.asList(myUserId), null, null);
            }
            RCRTCEngine.getInstance().leaveRoom(new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    isInRoom = false;
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {

                }
            });
//            if (renderViewManager != null)
            //                renderViewManager.destroyViews();

        }

        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        RCRTCMicOutputStream defaultAudioStream = RCRTCEngine.getInstance().getDefaultAudioStream();
        if (defaultAudioStream != null) {
            defaultAudioStream.setAudioDataListener(null);
        }

        RCRTCCameraOutputStream defaultVideoStream = RCRTCEngine.getInstance().getDefaultVideoStream();
        if (defaultVideoStream != null) {
            defaultVideoStream.setVideoFrameListener(null);
        }
    }

    public void startAudioMixActivity(Activity activity) {
        activity.startActivity(new Intent(activity, AudioMixActivity.class));
    }

    private OnRongYunConnectionMonitoring onRongYunConnectionMonitoring;

    //融云连接监听
    public void setOnRongYunConnectionMonitoring(OnRongYunConnectionMonitoring onRongYunConnectionMonitoring) {
        this.onRongYunConnectionMonitoring = onRongYunConnectionMonitoring;
    }


    public interface OnRongYunConnectionMonitoring {
        /**
         * 连接房间成功
         * isAdmin= true== 只有一个人，下一个页面需要设置管理员id
         */
        void onConnectionSucceeded(boolean isAdmin);

        //连接房间失败
        void onConnectionFailure(String err);
    }

    private void connectToRoom() {
        if (RongIMClient.getInstance().getCurrentConnectionStatus()
                == RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
            initOrUpdateRTCEngine();

            RCRTCRoomType roomType;
            if (UserUtils.IS_LIVE) {
                roomType = UserUtils.IS_VIDEO_MUTE ? RCRTCRoomType.LIVE_AUDIO : RCRTCRoomType.LIVE_AUDIO_VIDEO;
            } else {//会议
                roomType = RCRTCRoomType.MEETING;
            }
            //加入房间
            RCRTCEngine.getInstance().joinRoom(UserUtils.ROOMID, roomType, new IRCRTCResultDataCallback<RCRTCRoom>() {
                @Override
                public void onSuccess(RCRTCRoom room) {
                    //设置房间属性
                    RCRTCRoom rooms = RCRTCEngine.getInstance().getRoom();
                    int joinMode = RoomInfoMessage.JoinMode.AUDIO_VIDEO;
                    if (UserUtils.IS_VIDEO_MUTE) {
                        joinMode = RoomInfoMessage.JoinMode.AUDIO;
                    }
                    if (UserUtils.IS_OBSERVER) {
                        joinMode = RoomInfoMessage.JoinMode.OBSERVER;
                    }
                    String userId = rooms.getLocalUser().getUserId();
                    int remoteUserCount = rooms.getRemoteUsers() != null ? rooms.getRemoteUsers().size() : 0;
                    boolean isAdmin = remoteUserCount == 0;
                    RoomInfoMessage roomInfoMessage = new RoomInfoMessage(
                            userId, UserUtils.USER_NAME, joinMode, System.currentTimeMillis(), isAdmin);
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("userId", userId);
                        jsonObject.put("userName", UserUtils.USER_NAME);
                        jsonObject.put("joinMode", joinMode);
                        jsonObject.put("joinTime", System.currentTimeMillis());
                        jsonObject.put("master", isAdmin ? 1 : 0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    rooms.setRoomAttribute(userId, jsonObject.toString(), roomInfoMessage, new IRCRTCResultCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailed(RTCErrorCode errorCode) {

                        }
                    });
                    if (UserUtils.IS_BENDI) {
                        initManager();
                    }else {
                        startCallActivity(isAdmin);
                    }
                    onRongYunConnectionMonitoring.onConnectionSucceeded(isAdmin);

                    int userCount = room.getRemoteUsers().size();
                    Log.d(TAG, "onSuccess: 房间人数==" + userCount);
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                    String toastMsg;
                    if (errorCode == RTCErrorCode.ServerUserBlocked) {
                        toastMsg = UserUtils.activity.getResources().getString(R.string.rtc_dialog_forbidden_by_server);
                    } else {
                        toastMsg = UserUtils.activity.getResources().getString(R.string.join_room_failed);
                    }
                    onRongYunConnectionMonitoring.onConnectionFailure(toastMsg);
                }
            });
        } else {
            String toastMsg = UserUtils.activity.getResources().getString(R.string.im_connect_failed);
            Toast.makeText(UserUtils.activity, toastMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void initOrUpdateRTCEngine() {
        RCRTCConfig.Builder configBuilder = RCRTCConfig.Builder.create();
        boolean audioEncryption = false;
        boolean videoEncryption = false;
        if (UserUtils.KEY_USE_AV_SETTING) {
            RCRTCParamsType.VideoBitrateMode videoBitrateMode;
            if (TextUtils.equals(UserUtils.ENCODER_BIT_RATE_MODE, UserUtils.ENCODE_BIT_RATE_MODE_CQ)) {
                videoBitrateMode = RCRTCParamsType.VideoBitrateMode.CQ;
            } else if (TextUtils.equals(UserUtils.ENCODER_BIT_RATE_MODE, UserUtils.ENCODE_BIT_RATE_MODE_VBR)) {
                videoBitrateMode = RCRTCParamsType.VideoBitrateMode.VBR;
            } else {
                videoBitrateMode = RCRTCParamsType.VideoBitrateMode.CBR;
            }
            /* 是否启动 AudioRecord */
            configBuilder.enableMicrophone(true)
                    /* 是否采用双声道 */
                    .enableStereo(UserUtils.AUDIO_STEREO_ENABLE)
                    /* 设置麦克采集来源 */
                    .setAudioSource(UserUtils.AUDIO_SOURCE)
                    /* 设置音频码率 */
                    .setAudioBitrate(UserUtils.AUDIO_TRANSPORT_BIT_RATE)
                    /* 设置音频采样率 */
                    .setAudioSampleRate(UserUtils.AUDIO_SAMPLE_RATE)
                    /* 视频编码配置 */
                    .enableHardwareEncoder(UserUtils.ENCODER_TYPE_KEY)
                    .setHardwareEncoderColor(UserUtils.ENCODER_COLOR_FORMAT_VAL_KEY)
                    .enableEncoderTexture(UserUtils.ACQUISITION_MODE_KEY)
                    .enableHardwareEncoderHighProfile(UserUtils.ENCODER_LEVEL_KEY)
                    /* 视频解码配置 */
                    .enableHardwareDecoder(UserUtils.DECODER_TYPE_KEY)
                    .setHardwareDecoderColor(UserUtils.DECODER_COLOR_FORMAT_VAL_KEY)
                    /* 编码码率控制模式 */
                    .setHardwareEncoderBitrateMode(videoBitrateMode)
                    /* 开启自定义音频加解密 */
                    .enableAudioEncryption(UserUtils.AUDIO_ENCRYPTION)
                    /* 开启自定义视频加解密 */.enableVideoEncryption(UserUtils.VIDEO_ENCRYPTION)
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
        RCRTCEngine.getInstance().init(UserUtils.activity.getApplicationContext(), configBuilder.build());
        FinLog.d(TAG + "", "init --> over");

        RCRTCAudioStreamConfig.Builder audioConfigBuilder = RCRTCAudioStreamConfig.Builder.create();
        if (UserUtils.KEY_USE_AV_SETTING) {
            /* Audio Echo Cancel */
            audioConfigBuilder.setEchoCancel(RCRTCParamsType.AECMode.parseValue(UserUtils.AUDIO_ECHO_CANCEL_MODE))
                    .enableEchoFilter(UserUtils.AUDIO_ECHO_CANCEL_FILTER_ENABLE)
                    /* Audio Noise Suppression */
                    .setNoiseSuppression(RCRTCParamsType.NSMode.parseValue(UserUtils.AUDIO_NOISE_SUPPRESSION_MODE))
                    .setNoiseSuppressionLevel(RCRTCParamsType.NSLevel.parseValue(UserUtils.AUDIO_NOISE_SUPPRESSION_LEVEL))
                    .enableHighPassFilter(UserUtils.AUDIO_NOISE_HIGH_PASS_FILTER)
                    /* Audio AGC Config */
                    .enableAGCControl(UserUtils.AUDIO_AGC_CONTROL_ENABLE)
                    .enableAGCLimiter(UserUtils.AUDIO_AGC_LIMITER_ENABLE)
                    .setAGCTargetdbov(UserUtils.AUDIO_AGC_TARGET_DBOV)
                    .setAGCCompression(UserUtils.AUDIO_AGC_COMPRESSION)
                    .enablePreAmplifier(UserUtils.AUDIO_PRE_AMPLIFIER_ENABLE)
                    .setPreAmplifierLevel(UserUtils.AUDIO_PRE_AMPLIFIER_LEVEL);
            RCRTCAudioStreamConfig audioStreamConfig = UserUtils.IS_AUDIO_MUSIC ?
                    audioConfigBuilder.buildMusicMode() : audioConfigBuilder.buildDefaultMode();
            FinLog.d(TAG + "", "Audio --> enter");
            RCRTCEngine.getInstance().getDefaultAudioStream().setAudioConfig(audioStreamConfig);
        }

        RCRTCVideoStreamConfig.Builder videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create();
        // 如果开启了镜像翻转 VideoFrame，则应关闭镜像预览功能，否则会造成2次翻转效果
        RCRTCEngine.getInstance().getDefaultVideoStream().setPreviewMirror(!UserUtils.IS_MIRROR);
        /* 视频分辨率/码率 */
//        String maxBitRate = sm.getString(SettingActivity.BIT_RATE_MAX, "");
//        String minBitRate = sm.getString(SettingActivity.BIT_RATE_MIN);
//        if (!TextUtils.isEmpty(maxBitRate)) {
//            videoConfigBuilder.setMaxRate(Integer.parseInt(maxBitRate.substring(0, maxBitRate.length() - 4)));
//        }
//        if (!TextUtils.isEmpty(minBitRate)) {
//            videoConfigBuilder.setMinRate(Integer.parseInt(minBitRate.substring(0, minBitRate.length() - 4)));
//        }
        videoConfigBuilder.setVideoResolution(selectiveResolution(UserUtils.RESOLUTION))
                .setVideoFps(selectiveFrame(UserUtils.FPS));
        RCRTCEngine.getInstance().getDefaultVideoStream().
                enableTinyStream(UserUtils.IS_STREAM_TINY);
        RCRTCEngine.getInstance().getDefaultVideoStream().setCameraDisplayOrientation(
                UserUtils.CAPTURE_CAMERA_DISPLAY_ORIENTATION_KEY);
        RCRTCEngine.getInstance().getDefaultVideoStream().setFrameOrientation(
                UserUtils.CAPTURE_FRAME_ORIENTATION_KEY);
        RCRTCEngine.getInstance().getDefaultVideoStream().setVideoConfig(videoConfigBuilder.build());
    }

    /**
     * 构造分辨率对应的BlinkVideoProfile对象
     *
     * @param resolutionStr
     * @return
     */
    private RCRTCParamsType.RCRTCVideoResolution selectiveResolution(String resolutionStr) {
        RCRTCParamsType.RCRTCVideoResolution profile = null;
        if (resolutionStr == null || resolutionStr.equals("")) {
            return RCRTCParamsType.RCRTCVideoResolution.RESOLUTION_480_640;
        }
        String[] resolutionArray = resolutionStr.split("x");
        profile = RCRTCParamsType.RCRTCVideoResolution.parseVideoResolution(
                Integer.parseInt(resolutionArray[0]), Integer.parseInt(resolutionArray[1]));
        return profile;
    }

    private RCRTCParamsType.RCRTCVideoFps selectiveFrame(String frameStr) {
        frameStr = TextUtils.isEmpty(frameStr) ? "15" : frameStr;
        return RCRTCParamsType.RCRTCVideoFps.parseVideoFps(Integer.parseInt(frameStr));
    }

    private boolean HeadsetPlugReceiverState = false; // false：开启音视频之前已经连接上耳机

    private void startCallActivity(boolean isRemoteUser) {
        Intent intent = null;
//        使用会议界面进行音视频
        intent = new Intent(UserUtils.activity, CallActivity.class);
        intent.putExtra(CallActivity.EXTRA_IS_MASTER, isRemoteUser);
        intent.putExtra("DATA", data);
        UserUtils.activity.startActivity(intent);
    }


    private void initManager() {
        HeadsetPlugReceiver.setOnHeadsetPlugListener(onHeadsetPlugListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        headsetPlugReceiver = new HeadsetPlugReceiver(BluetoothUtil.hasBluetoothA2dpConnected());
        UserUtils.activity.registerReceiver(headsetPlugReceiver, intentFilter);
        myUserId = RongIMClient.getInstance().getCurrentUserId();
        if (RCRTCEngine.getInstance().getRoom().getRemoteUsers().size() == 0) {
            adminUserId = myUserId;
        }
//        initAudioManager();
        startCall();
        room.getRoomAttributes(null, new IRCRTCResultDataCallback<Map<String, String>>() {
            @Override
            public void onSuccess(final Map<String, String> data) {
                UserUtils.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onGetRoomAttributesHandler(data);
                    }
                });
            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {

            }
        });
    }

    private void onGetRoomAttributesHandler(Map<String, String> data) {
        try {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                FinLog.d(TAG, "[MemberList] onCreate ==>  " + entry);
                JSONObject jsonObject = new JSONObject(entry.getValue());
                UserInfo userInfo = new UserInfo();
                userInfo.userName = jsonObject.getString("userName");
                userInfo.joinMode = jsonObject.getInt("joinMode");
                userInfo.userId = jsonObject.getString("userId");
                userInfo.timestamp = jsonObject.getLong("joinTime");
                boolean master = jsonObject.optInt("master") == 1;
                if (master) {
                    adminUserId = userInfo.userId;
                }
                if (room.getRemoteUser(userInfo.userId) == null && !TextUtils.equals(myUserId, userInfo.userId)) {
                    continue;
                }
                if (mMembersMap.containsKey(entry.getKey())) {
                    continue;
                }
                mMembersMap.put(entry.getKey(), userInfo);

                ItemModel model = new ItemModel();
                model.mode = mapMode(userInfo.joinMode);
                model.name = userInfo.userName;
                model.userId = userInfo.userId;
                model.joinTime = userInfo.timestamp;
                mMembers.add(model);

            }
            FinLog.d(TAG, "[MemberList] getRoomAttributes ==>  MemberSize=" + mMembers.size());
            sortRoomMembers();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initAudioManager() {
        UserUtils.activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager =
                AppRTCAudioManager.create(
                        UserUtils.activity.getApplicationContext(),
                        new Runnable() {
                            // This method will be called each time the audio state (number and
                            // type of devices) has been changed.
                            @Override
                            public void run() {
//                                onAudioManagerChangedState();
                            }
                        });
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Initializing the audio manager...");
        audioManager.init();
    }

    //启动蓝牙

    private void startBluetoothSco() {
        AudioManager am = (AudioManager) UserUtils.activity.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            if (am.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
                am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
            am.setSpeakerphoneOn(false);
            am.startBluetoothSco();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startCall() {
        try {
            room = RCRTCEngine.getInstance().getRoom();
            room.registerRoomListener(roomEventsListener);
            localUser = room.getLocalUser();

            RCRTCEngine.getInstance().getDefaultAudioStream().setAudioDataListener(audioDataListener);
            RCRTCEngine.getInstance().getDefaultAudioStream().setMicrophoneDisable(muteMicrophone);
            RongCallClient.getInstance().setEnableSpeakerphone(true);
            publishResource(); // 发布资源
            subscribeAll();

            if (!HeadsetPlugReceiverState) {
                int type = -1;
                if (BluetoothUtil.hasBluetoothA2dpConnected()) {
                    type = 0;
                } else if (BluetoothUtil.isWiredHeadsetOn(UserUtils.activity)) {
                    type = 1;
                }
                if (type != -1) {
                    onHeadsetPlugListener.onNotifyHeadsetState(true, type);
                }
            }
            isInRoom = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void publishResource() {
        if (UserUtils.IS_OBSERVER) {
            return;
        }
        if (localUser == null) {
            Toast.makeText(UserUtils.activity, "不在房间里", Toast.LENGTH_SHORT).show();
            return;
        }

        if (RongIMClient.getInstance().getCurrentConnectionStatus() == RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE) {
            String toastMsg = UserUtils.activity.getResources().getString(R.string.Thecurrentnetworkisnotavailable);
            Toast.makeText(UserUtils.activity, toastMsg, Toast.LENGTH_SHORT).show();
            return;
        }

        final List<RCRTCOutputStream> localAvStreams = new ArrayList<>();
        localAvStreams.add(RCRTCEngine.getInstance().getDefaultAudioStream());
        if (!UserUtils.IS_LIVE) {
            if (UserUtils.IS_BENDI) {//   先发布本地流 音频   再发布共享屏幕视频流
                localUser.publishStreams(localAvStreams, new IRCRTCResultCallback() {
                    @Override
                    public void onSuccess() {
                        FinLog.v(TAG, "publish success()");
                        RCRTCVideoStreamConfig.Builder videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create();
                        videoConfigBuilder.setVideoResolution(RCRTCParamsType.RCRTCVideoResolution.RESOLUTION_720_1280);
                        videoConfigBuilder.setVideoFps(RCRTCParamsType.RCRTCVideoFps.Fps_10);
                        screenOutputStream = RCRTCEngine.getInstance()
                                .createVideoStream(RongRTCScreenCastHelper.VIDEO_TAG, videoConfigBuilder.build());
                        screenCastHelper = new RongRTCScreenCastHelper();
                        screenCastHelper.init(UserUtils.activity, screenOutputStream, data, 720, 1280);
                        RCRTCVideoView videoView = new RCRTCVideoView(UserUtils.activity);
                        screenOutputStream.setVideoView(videoView);
                        screenCastHelper.start();
                        localUser.publishStream(screenOutputStream, new IRCRTCResultCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "onSuccess: 发布共享屏幕成功");
                            }

                            @Override
                            public void onFailed(RTCErrorCode errorCode) {
                                UserUtils.activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String toastInfo = UserUtils.activity.getResources().getString(R.string.publish_failed);
                                        Toast.makeText(UserUtils.activity, toastInfo, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onFailed(RTCErrorCode errorCode) {
                        FinLog.e(TAG, "publish publish Failed()");
                        // 50010 网络请求超时错误时，重试一次资源发布操作
                        if (errorCode.equals(RTCErrorCode.RongRTCCodeHttpTimeoutError)) {
                            publishResource();
                        }
                    }
                });
            } else {//发布音频  可以通话
                localUser.publishStreams(localAvStreams, new IRCRTCResultCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "onSuccess: 发布音频成功");
                    }

                    @Override
                    public void onFailed(RTCErrorCode errorCode) {
                        FinLog.e(TAG, "publish publish Failed()");
                        // 50010 网络请求超时错误时，重试一次资源发布操作
                        if (errorCode.equals(RTCErrorCode.RongRTCCodeHttpTimeoutError)) {
                            localUser.publishStreams(localAvStreams, null);
                        }
                    }
                });
            }
            return;
        }
    }

    //订阅所有流
    private void subscribeAll() {
        for (final RCRTCRemoteUser remoteUser : room.getRemoteUsers()) {
            if (remoteUser.getStreams().size() == 0) {
                continue;
            }
            localUser.subscribeStreams(remoteUser.getStreams(), new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "成功订阅");
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                    FinLog.d(TAG, "subscribeAll subscribeStreams userId = " + remoteUser.getUserId() + ", errorCode =" + errorCode.getValue());
                    // 50010 网络请求超时错误时，重试一次订阅操作
                    if (RTCErrorCode.RongRTCCodeHttpTimeoutError.equals(errorCode) && remoteUser.getStreams() != null && remoteUser.getStreams().size() > 0) {
                        localUser.subscribeStreams(remoteUser.getStreams(), null);
                    }
                }
            });
        }
    }

    private OnHeadsetPlugListener onHeadsetPlugListener = new OnHeadsetPlugListener() {
        @Override
        public void onNotifyHeadsetState(boolean connected, int type) {
            try {
                if (connected) {
                    HeadsetPlugReceiverState = true;
                    if (type == 0) {
                        startBluetoothSco();
                    }
                } else {
                    if (type == 1 && BluetoothUtil.hasBluetoothA2dpConnected()) {
                        return;
                    }
                    AudioManager am = (AudioManager) UserUtils.activity.getSystemService(Context.AUDIO_SERVICE);
                    if (am != null) {
                        if (am.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
                            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        }
                        if (type == 0) {
                            am.stopBluetoothSco();
                            am.setBluetoothScoOn(false);
                            am.setSpeakerphoneOn(!muteSpeaker);
                        } else {
                            RCRTCEngine.getInstance().enableSpeaker(!muteSpeaker);
                        }
                        audioManager.onToggleSpeaker(!muteSpeaker);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onNotifySCOAudioStateChange(int scoAudioState) {
            switch (scoAudioState) {
                case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                    AudioManager am = (AudioManager) UserUtils.activity.getSystemService(Context.AUDIO_SERVICE);
                    if (am != null) {
                        am.setBluetoothScoOn(true);
                    }
                    break;
                case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                    Log.d("onNotifyHeadsetState", "onNotifySCOAudioStateChange: " + headsetPlugReceiver.isBluetoothConnected());
                    if (headsetPlugReceiver.isBluetoothConnected()) {
                        startBluetoothSco();
                    }
                    break;
            }
        }
    };

    private IRCRTCRoomEventsListener roomEventsListener = new IRCRTCRoomEventsListener() {
        @Override
        public void onRemoteUserPublishResource(final RCRTCRemoteUser remoteUser, List<RCRTCInputStream> streams) {
            FinLog.d(TAG, "--- onRemoteUserPublishResource ----- remoteUser: " + remoteUser);
            if (remoteUser == null) {
                return;
            }

            UserUtils.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertRemotePublished(remoteUser);
                }
            });

            room.getRoomAttributes(null, new IRCRTCResultDataCallback<Map<String, String>>() {
                @Override
                public void onSuccess(final Map<String, String> data) {
                    UserUtils.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // 房间中有用户断线1分钟后重连，会直接发布资源，这是重新统计房间人员信息
                                if (mMembersMap == null || mMembersMap.size() == data.size()) {
                                    return;
                                }
                                mMembersMap.clear();
                                mMembers.clear();
                                for (Map.Entry<String, String> entry : data.entrySet()) {
                                    FinLog.d(TAG, "[MemberList] onRemoteUserPublishResource ==>  " + entry);
                                    JSONObject jsonObject = new JSONObject(entry.getValue());
                                    UserInfo userInfo = new UserInfo();
                                    userInfo.userName = jsonObject.getString("userName");
                                    userInfo.joinMode = jsonObject.getInt("joinMode");
                                    userInfo.userId = jsonObject.getString("userId");
                                    userInfo.timestamp = jsonObject.getLong("joinTime");
                                    boolean master = jsonObject.optInt("master") == 1;
                                    if (master) {
                                        adminUserId = userInfo.userId;
                                    }

                                    if (room.getRemoteUser(userInfo.userId) == null && !TextUtils.equals(myUserId, userInfo.userId)) {
                                        continue;
                                    }
                                    mMembersMap.put(entry.getKey(), userInfo);

                                    ItemModel model = new ItemModel();
                                    model.mode = mapMode(userInfo.joinMode);
                                    model.name = userInfo.userName;
                                    model.userId = userInfo.userId;
                                    model.joinTime = userInfo.timestamp;
                                    mMembers.add(model);

                                }
                                FinLog.d(TAG, "[MemberList] onRemoteUserPublishResource ==>  MemberSize=" + mMembers.size());
                                sortRoomMembers();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    });

                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                }
            });
        }

        @Override
        public void onKickedByServer() {

        }

        @Override
        public void onVideoTrackAdd(final String userId, final String tag) {
            Log.i(TAG, "onVideoTrackAdd() userId: " + userId + " ,tag = " + tag);
            UserUtils.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (TextUtils.equals(tag, RongRTCScreenCastHelper.VIDEO_TAG)) {
                        screenCastEnable = false;
                    }
                }
            });
        }

        @Override
        public void onReceiveMessage(final io.rong.imlib.model.Message message) {
            UserUtils.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MessageContent messageContent = message.getContent();
                    FinLog.i(TAG, "onReceiveMessage()->" + messageContent);
                    if (messageContent instanceof RoomInfoMessage) {
                        RoomInfoMessage roomInfoMessage = (RoomInfoMessage) messageContent;
                        FinLog.d(TAG, "[MemberList] onReceiveMessage ==>  " + new String(roomInfoMessage.encode()));
                        ItemModel itemModel = new ItemModel();
                        itemModel.name = roomInfoMessage.getUserName();
                        itemModel.mode = mapMode(roomInfoMessage.getJoinMode());
                        itemModel.userId = roomInfoMessage.getUserId();
                        itemModel.joinTime = roomInfoMessage.getTimeStamp();
                        if (!mMembersMap.containsKey(itemModel.userId)) {
                            String toastMsg = itemModel.name + " " + UserUtils.activity.getResources().getString(R.string.rtc_join_room);
                            Toast.makeText(UserUtils.activity, toastMsg, Toast.LENGTH_SHORT).show();
                            mMembers.add(0, itemModel);
                            sortRoomMembers();
                        } else {
                            for (ItemModel member : mMembers) {
                                if (TextUtils.equals(member.userId, itemModel.userId)) {
                                    member.mode = itemModel.mode;
                                    break;
                                }
                            }
                            if (roomInfoMessage.isMaster() && !itemModel.userId.equals(adminUserId)) {
                                adminUserId = itemModel.userId;
                                if (itemModel.userId.equals(myUserId)) {
                                    String toastMsg = UserUtils.activity.getResources().getString(R.string.member_operate_admin_me);
                                    Toast.makeText(UserUtils.activity, toastMsg, Toast.LENGTH_SHORT).show();
                                } else {
                                    String toastMsg = itemModel.name + " " + UserUtils.activity.getResources().getString(R.string.member_operate_admin_new);
                                    Toast.makeText(UserUtils.activity, toastMsg, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        UserInfo userInfo = new UserInfo();
                        userInfo.userId = roomInfoMessage.getUserId();
                        userInfo.userName = roomInfoMessage.getUserName();
                        userInfo.joinMode = roomInfoMessage.getJoinMode();
                        userInfo.timestamp = roomInfoMessage.getTimeStamp();
                        mMembersMap.put(roomInfoMessage.getUserId(), userInfo);

                    } else if (messageContent instanceof RoomKickOffMessage) {
                        RoomKickOffMessage kickOffMessage = (RoomKickOffMessage) messageContent;
                        if (myUserId.equals(kickOffMessage.getUserId())) {
                            FinLog.i(TAG, "kickOffMessage-intendToLeave");
                            intendToLeave(false);
                        }
                    }
                }
            });
        }

        @Override
        public void onFirstRemoteVideoFrame(final String userId, final String tag) {
            Log.i(TAG, "onFirstFrameDraw() userId: " + userId + " ,tag = " + tag);
        }

        @Override
        public void onRemoteUserMuteAudio(RCRTCRemoteUser remoteUser, RCRTCInputStream stream, boolean mute) {
            FinLog.d(TAG, "onRemoteUserAudioStreamMute remoteUser: " + remoteUser + " ,  mute :" + mute);
        }

        @Override
        public void onRemoteUserMuteVideo(final RCRTCRemoteUser remoteUser, final RCRTCInputStream stream, final boolean mute) {
            FinLog.d(TAG, "onRemoteUserVideoStreamEnabled remoteUser: " + remoteUser + "  , enable :" + mute);
            if (remoteUser == null || stream == null) {
                return;
            }
        }

        @Override
        public void onRemoteUserUnpublishResource(final RCRTCRemoteUser remoteUser, final List<RCRTCInputStream> streams) {
            FinLog.d(TAG, "onRemoteUserUnpublishResource remoteUser: " + remoteUser);
            if (streams == null) {
                return;
            }
            UserUtils.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (RCRTCInputStream stream : streams) {
                        if (stream.getMediaType().equals(RCRTCMediaType.VIDEO)) {
                            if (TextUtils.equals(stream.getTag(), RongRTCScreenCastHelper.VIDEO_TAG)) {
                                screenCastEnable = true;
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onUserJoined(RCRTCRemoteUser remoteUser) {
            FinLog.d(TAG, "onUserJoined  remoteUser :" + remoteUser.getUserId());
        }

        @Override
        public void onUserLeft(final RCRTCRemoteUser remoteUser) {
            UserUtils.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UserUtils.activity, getUserName(remoteUser.getUserId()) + " " + UserUtils.activity.getResources().getString(R.string.rtc_quit_room), Toast.LENGTH_SHORT).show();
                    exitRoom(remoteUser.getUserId());
                }
            });
        }

        @Override
        public void onUserOffline(final RCRTCRemoteUser remoteUser) {
            UserUtils.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UserUtils.activity, getUserName(remoteUser.getUserId()) + " " + UserUtils.activity.getResources().getString(R.string.rtc_user_offline), Toast.LENGTH_SHORT).show();
                    exitRoom(remoteUser.getUserId());
                    if (remoteUser.getUserId().equals(adminUserId)) {
                        adminUserId = null;
                    }
                }
            });
        }

        @Override
        public void onLeaveRoom(int reasonCode) {
            UserUtils.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final PromptDialog dialog = PromptDialog.newInstance(UserUtils.activity, UserUtils.activity.getString(R.string.rtc_status_im_error));
                    dialog.setPromptButtonClickedListener(new PromptDialog.OnPromptButtonClickedListener() {
                        @Override
                        public void onPositiveButtonClicked() {
                            onStop();
                            onDestroy();
                        }

                        @Override
                        public void onNegativeButtonClicked() {
                        }
                    });
                    dialog.disableCancel();
                    dialog.setCancelable(false);
                    dialog.show();
                }
            });
        }
    };

    private void alertRemotePublished(final RCRTCRemoteUser remoteUser) {
        Log.i(TAG, "alertRemotePublished()警报远程发布= start");
        localUser.subscribeStreams(remoteUser.getStreams(), new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {
                FinLog.d(TAG, "subscribeStreams userId = " + remoteUser.getUserId() + ", errorCode =" + errorCode.getValue());
                // 50010 网络请求超时错误时，重试一次订阅操作
                if (RTCErrorCode.RongRTCCodeHttpTimeoutError.equals(errorCode) && remoteUser.getStreams() != null &&
                        remoteUser.getStreams().size() > 0) {
                    localUser.subscribeStreams(remoteUser.getStreams(), null);
                }
            }
        });
    }

    private String getUserName(String userId) {
        if (TextUtils.isEmpty(userId)) return userId;
        UserInfo userInfo = mMembersMap.get(userId);
        if (userInfo == null) return userId;
        return userInfo.userName;
    }

    private String mapMode(int mode) {
        if (mode == RoomInfoMessage.JoinMode.AUDIO) {
            return UserUtils.activity.getString(R.string.mode_audio);
        } else if (mode == RoomInfoMessage.JoinMode.AUDIO_VIDEO) {
            return UserUtils.activity.getString(R.string.mode_audio_video);
        } else if (mode == RoomInfoMessage.JoinMode.OBSERVER) {
            return UserUtils.activity.getString(R.string.mode_observer);
        }
        return "";
    }


    private void selectAdmin() {
        if (!TextUtils.equals(myUserId, adminUserId) || mMembersMap.size() <= 1) return;
        UserInfo userInfo = mMembersMap.get(mMembers.get(1).userId);
        if (userInfo == null) return;
        RoomInfoMessage roomInfoMessage = new RoomInfoMessage(
                userInfo.userId, userInfo.userName, userInfo.joinMode, userInfo.timestamp, true);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userId", userInfo.userId);
            jsonObject.put("userName", userInfo.userName);
            jsonObject.put("joinMode", userInfo.joinMode);
            jsonObject.put("joinTime", userInfo.timestamp);
            jsonObject.put("master", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        room.setRoomAttribute(userInfo.userId, jsonObject.toString(), roomInfoMessage, null);
    }

    private void disconnect() {
        isConnected = false;
        if (room != null) {
            room.deleteRoomAttributes(Collections.singletonList(myUserId), null, null);
        }
        RCRTCEngine.getInstance().leaveRoom(new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {
                UserUtils.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FinLog.i(TAG, "quitRoom()->onUiSuccess");
                        isInRoom = false;
                        if (!kicked) {
                            Toast.makeText(UserUtils.activity, UserUtils.activity.getResources().getString(R.string.quit_room_success), Toast.LENGTH_SHORT).show();
                        }
                        if (audioManager != null) {
                            audioManager.close();
                            audioManager = null;
                        }
                        UserUtils.CONNECTION_STATUS = 0;
                        onStop();
                        onDestroy();
                    }
                });
            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {
                FinLog.i(TAG, "quitRoom()->onUiFailed : " + errorCode);
                UserUtils.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (audioManager != null) {
                            audioManager.close();
                            audioManager = null;
                        }
                        UserUtils.CONNECTION_STATUS = 0;
                        onStop();
                        onDestroy();
                    }
                });


            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void cancelScreenCast(final boolean isHangup) {
        if (screenOutputStream == null || screenCastHelper != null) {
            return;
        }
        screenCastHelper.stop();
        screenCastHelper = null;
        localUser.unpublishStream(screenOutputStream, new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {
                UserUtils.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        screenOutputStream = null;
                        if (isHangup) {
                            disconnect();
                        }
                    }
                });
            }

            @Override
            public void onFailed(final RTCErrorCode errorCode) {
                UserUtils.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        screenOutputStream = null;
                        if (isHangup) {
                            disconnect();
                        }
                    }
                });
            }
        });
        screenOutputStream = null;
        if (isHangup) {
            disconnect();
        }
    }


    private IRCRTCAudioDataListener audioDataListener = new IRCRTCAudioDataListener() {
        @Override
        public byte[] onAudioFrame(RCRTCAudioFrame rtcAudioFrame) {
            return rtcAudioFrame.getBytes();
        }
    };

    private void sortRoomMembers() {
        Collections.sort(
                mMembers,
                new Comparator<ItemModel>() {
                    @Override
                    public int compare(ItemModel o1, ItemModel o2) {
                        return (int) (o1.joinTime - o2.joinTime);
                    }
                });
        // 如果第一的位置不是管理员，强制把管理员排到第一的位置
        if (mMembers.size() > 0 && !mMembers.get(0).userId.equals(adminUserId)) {
            ItemModel adminItem = null;
            for (ItemModel model : mMembers) {
                if (model.userId.equals(adminUserId)) {
                    adminItem = model;
                    break;
                }
            }
            if (adminItem != null) {
                mMembers.remove(adminItem);
                mMembers.add(0, adminItem);
            }
        }
    }

    private void exitRoom(String userId) {
        mMembersMap.remove(userId);
        for (int i = mMembers.size() - 1; i >= 0; --i) {
            ItemModel model = mMembers.get(i);
            if (TextUtils.equals(model.userId, userId)) {
                mMembers.remove(i);
                break;
            }
        }
    }


}