package com.my.mylibrary;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.my.mylibrary.bean.ItemModel;
import com.my.mylibrary.bean.UserInfo;
import com.my.mylibrary.broadcast.HomeWatcherReceiver;
import com.my.mylibrary.call.AppRTCAudioManager;
import com.my.mylibrary.dialog.PromptDialog;
import com.my.mylibrary.message.RoomInfoMessage;
import com.my.mylibrary.message.RoomKickOffMessage;
import com.my.mylibrary.screen_cast.RongRTCScreenCastHelper;
import com.my.mylibrary.screen_cast.ScreenCastService;
import com.my.mylibrary.utils.BluetoothUtil;
import com.my.mylibrary.utils.HeadsetPlugReceiver;
import com.my.mylibrary.utils.OnHeadsetPlugListener;
import com.my.mylibrary.utils.SessionManager;
import com.my.mylibrary.utils.UserUtils;
import com.my.mylibrary.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
import cn.rongcloud.rtc.api.stream.RCRTCInputStream;
import cn.rongcloud.rtc.api.stream.RCRTCMicOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoStreamConfig;
import cn.rongcloud.rtc.api.stream.RCRTCVideoView;
import cn.rongcloud.rtc.base.RCRTCAudioFrame;
import cn.rongcloud.rtc.base.RCRTCParamsType;
import cn.rongcloud.rtc.base.RCRTCRoomType;
import cn.rongcloud.rtc.base.RTCErrorCode;
import cn.rongcloud.rtc.utils.FinLog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.MessageContent;

import static android.content.Context.BIND_AUTO_CREATE;


/**
 * Description:
 * <p>
 * CreateTime：2021/02/20  17:03
 */
public class RongRTC {
    //房间
    private RCRTCRoom room;
    private RCRTCLocalUser localUser;
    private boolean kicked = false;
    //屏幕投射助手
    private RongRTCScreenCastHelper screenCastHelper;
    //共享屏幕的流
    private RCRTCVideoOutputStream screenOutputStream;
    private OnRongYunConnectionMonitoring onRongYunConnectionMonitoring;
    private List<ItemModel> mMembers = new ArrayList<>();
    private List<Activity> activityList = new ArrayList<>();
    private Map<String, UserInfo> mMembersMap = new HashMap<>();
    //我的userID；
    private String myUserId;
    private String adminUserId;
    /**
     * true 关闭扬声器； false 打开扬声器
     */
    private boolean muteSpeaker = false;
    //音频
    private AppRTCAudioManager audioManager = null;
    //监听耳机
    private HeadsetPlugReceiver headsetPlugReceiver = null;
    public Intent data;
    public int MEDIA_PROJECTION_SERVICE_CODE = 101;

    public static String TAG = "RongRTC>>>>>>";
    private static RongRTC rongRTC;
    private ServiceConnection serviceConnection = null;
    // false：开启音视频之前已经连接上耳机
    private boolean HeadsetPlugReceiverState = false;

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
     * 初始化融云连接监听
     */
    public void initConnectionStatusListener() {
        RongIMClient.setConnectionStatusListener(new RongIMClient.ConnectionStatusListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onChanged(ConnectionStatus connectionStatus) {
                if (connectionStatus == ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT) {
                    discontinueSharing("此用户被其他设备登录");
                }
            }
        });
    }

    /**
     * 融云连接监听
     */
    public void setOnRongYunConnectionMonitoring(OnRongYunConnectionMonitoring onRongYunConnectionMonitoring) {
        this.onRongYunConnectionMonitoring = onRongYunConnectionMonitoring;
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

    /**
     * 是否已连接融云
     *
     * @return
     */
    public boolean isConnected() {
        if (!TextUtils.isEmpty(UserUtils.TOKEN) && RongIMClient.getInstance().getCurrentConnectionStatus() == RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)
            return true;
        return false;
    }

    //添加activity
    public void addActivity(Activity activity) {
        activityList.add(activity);
    }

    public List<Activity> getActivityList() {
        return activityList;
    }

    /**
     * 开始连接融云
     *
     * @param activity
     * @param datas    共享屏幕必传
     * @param token    token
     * @param roomId   房间id
     * @param username 用户名
     */
    public void start(Activity activity, Intent datas, String token, String roomId, String username) {
        if (datas != null) {
            data = datas;
            UserUtils.IS_BENDI = true;
        } else {
            UserUtils.IS_BENDI = false;
        }
        initConnectionStatusListener();
        UserUtils.activity = activity;
        UserUtils.ROOMID = roomId;
        UserUtils.USER_NAME = username;
        HomeWatcherReceiver.registerHomeKeyReceiver(UserUtils.activity);//监听Home键
        if (isConnected() && UserUtils.TOKEN.equals(token)) {//如果已连接融云并且是一个token 直接加入房间
            connectToRoom();
            return;
        }
        UserUtils.TOKEN = token;
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
                    onRongYunConnectionMonitoring.onTokenFail();
                } else if (errorCode == RongIMClient.ConnectionErrorCode.RC_CONNECTION_EXIST) {
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
    public void intendToLeave(boolean initiative, String reason) {
        FinLog.i(TAG, "intendToLeave()-> " + initiative);
        if (initiative) {
            selectAdmin();
        } else {
            kicked = true;
        }
        RCRTCAudioMixer.getInstance().stop();
        // 当前用户是观察者 或 离开房间时还有其他用户存在，直接退出
        disconnect(reason);
    }

    public void discontinueSharing(String reason) {
        onRongYunConnectionMonitoring.onDestroyed(reason);
    }

    /**
     * 销毁实例
     */
    public void onDestroy() {
        RongIMClient.getInstance().disconnect();
        RongIMClient.getInstance().logout();
        RCRTCEngine.getInstance().unInit();
        HomeWatcherReceiver.unregisterHomeKeyReceiver(UserUtils.activity);
        if (serviceConnection != null) {
            UserUtils.activity.unbindService(serviceConnection);
            serviceConnection = null;
        }
        HeadsetPlugReceiver.setOnHeadsetPlugListener(null);
        if (headsetPlugReceiver != null) {
            UserUtils.activity.unregisterReceiver(headsetPlugReceiver);
            headsetPlugReceiver = null;
        }
        if (room != null) {
            room.unregisterRoomListener();
            room = null;
        }
        RCRTCEngine.getInstance().unregisterStatusReportListener();
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        RCRTCMicOutputStream defaultAudioStream = RCRTCEngine.getInstance().getDefaultAudioStream();
        if (defaultAudioStream != null) {
            defaultAudioStream.setAudioDataListener(null);
        }
        if (activityList != null && activityList.size() > 0) {
            for (int i = 0; i < activityList.size(); i++) {
                activityList.get(i).finish();
            }
        }
        empty();
    }

    public void empty() {
        if (rongRTC != null) {
            System.gc();
            rongRTC = null;
        }
    }

    /**
     * 选择管理员
     */
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

    /**
     * 离开房间
     */
    private void disconnect(String reason) {
        if (room != null) {
            room.deleteRoomAttributes(Collections.singletonList(myUserId), null, null);
        }
        RCRTCEngine.getInstance().leaveRoom(new IRCRTCResultCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onSuccess() {
                if (UserUtils.IS_BENDI) {
                    cancelScreenCast();
                }
                UserUtils.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FinLog.i(TAG, "quitRoom()->onUiSuccess");
                        if (!kicked) {
                            onRongYunConnectionMonitoring.onSuccessfullyExitTheRoom(reason);
                        }
                        if (audioManager != null) {
                            audioManager.close();
                            audioManager = null;
                        }
                        onDestroy();
                    }
                });
            }

            @Override
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onFailed(RTCErrorCode errorCode) {
                FinLog.i(TAG, "quitRoom()->onUiFailed : " + errorCode);
                if (UserUtils.IS_BENDI) {
                    cancelScreenCast();
                }
                UserUtils.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!kicked) {
                            onRongYunConnectionMonitoring.onSuccessfullyExitTheRoom(reason);
                        }
                        if (audioManager != null) {
                            audioManager.close();
                            audioManager = null;
                        }
                        onDestroy();
                    }
                });
            }
        });
    }

    /**
     * 连接房间
     */
    private void connectToRoom() {
        if (isConnected()) {
            initOrUpdateRTCEngine();
            RCRTCRoomType roomType = RCRTCRoomType.MEETING;
            //加入房间
            RCRTCEngine.getInstance().joinRoom(UserUtils.ROOMID, roomType, new IRCRTCResultDataCallback<RCRTCRoom>() {
                @Override
                public void onSuccess(RCRTCRoom room) {
                    if (!UserUtils.IS_BENDI && room.getRemoteUsers().size() >= 5) {//设置如果超过5人不能发布音视频
                        UserUtils.IS_OBSERVER = true;
                    }
                    RCRTCRoom rooms = RCRTCEngine.getInstance().getRoom();
                    int joinMode = RoomInfoMessage.JoinMode.AUDIO_VIDEO;
                    String userId = rooms.getLocalUser().getUserId();
                    int remoteUserCount = rooms.getRemoteUsers() != null ? rooms.getRemoteUsers().size() : 0;
                    boolean isAdmin = remoteUserCount == 0;
                    RoomInfoMessage roomInfoMessage = new RoomInfoMessage(userId, UserUtils.USER_NAME, joinMode, System.currentTimeMillis(), isAdmin);
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
                    //设置跟随系统音量
                    AudioManager am = (AudioManager) UserUtils.activity.getSystemService(Context.AUDIO_SERVICE);
                    if (am != null) {
                        if (am.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
                            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        }
                    }
                    if (UserUtils.IS_BENDI)
                        initManager();
                    onRongYunConnectionMonitoring.onConnectedToTheRoomSuccessfully(UserUtils.IS_BENDI, isAdmin);
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                    String toastMsg;
                    if (errorCode == RTCErrorCode.ServerUserBlocked) {
                        toastMsg = UserUtils.activity.getResources().getString(R.string.rtc_dialog_forbidden_by_server);
                    } else {
                        toastMsg = UserUtils.activity.getResources().getString(R.string.join_room_failed);
                    }
                    onRongYunConnectionMonitoring.onFailedToConnectToRoom(toastMsg);
                }
            });
        } else {
            onRongYunConnectionMonitoring.onConnectionRongYunFailed("IM还未建立连接");
        }
    }


    private void initOrUpdateRTCEngine() {
        RCRTCConfig.Builder configBuilder = RCRTCConfig.Builder.create();
        RCRTCParamsType.VideoBitrateMode videoBitrateMode;
//            videoBitrateMode = RCRTCParamsType.VideoBitrateMode.CQ;
//            videoBitrateMode = RCRTCParamsType.VideoBitrateMode.VBR;
        videoBitrateMode = RCRTCParamsType.VideoBitrateMode.CBR;

        /* 是否启动 AudioRecord */
        configBuilder.enableMicrophone(true)
                /* 是否采用双声道 */
                .enableStereo(false)
                /* 设置麦克采集来源
                 * 默认设置的音源在设备上 AudioRecord 采集音频异常场景  必须使用 MediaRecorder.AudioSource 类参数值，默认为 MediaRecorder.AudioSource.VOICE_COMMUNICATION
                 * */
                .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                /* 设置音频码率 */
                .setAudioBitrate(40)
                /* 设置音频采样率 */
                .setAudioSampleRate(48000)
                /* 视频编码配置 */
                .enableHardwareEncoder(true)
                //设置硬件编码器颜色
                .setHardwareEncoderColor(0)
                //启用编码器纹理
                .enableEncoderTexture(true)
                //启用硬件编码器高级配置文件
                .enableHardwareEncoderHighProfile(false)
                /* 视频解码配置 */
                .enableHardwareDecoder(true)
                //设置硬件解码器颜色
                .setHardwareDecoderColor(0)
                /* 编码码率控制模式 */
                .setHardwareEncoderBitrateMode(videoBitrateMode)
                /* 开启自定义音频加解密 */
                .enableAudioEncryption(false)
                /* 开启自定义视频加解密 */
                .enableVideoEncryption(false)
                /* 开启SRTP*/
                .enableSRTP(false);
        //自定义加解密 so 加载
//            CustomizedEncryptionUtil.getInstance().init();

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
        //AUDIO ECHO取消模式  回声消除使用ACE和AECM两种处理算法。其中ACE的算法复杂度比AECM要高，回声消除的效果回较明显。
        audioConfigBuilder.setEchoCancel(RCRTCParamsType.AECMode.parseValue(2))//RCRTCParamsType.AECMode.parseValue(UserUtils.AUDIO_ECHO_CANCEL_MODE)
                //设置回声扩展滤波器是否可用，默认 false
                .enableEchoFilter(true)
                //设置噪声抑制算法方案，默认为 NSMode.NS_MODE0 (不采用降噪处理)
                .setNoiseSuppression(RCRTCParamsType.NSMode.parseValue(3))//RCRTCParamsType.NSMode.parseValue(UserUtils.AUDIO_NOISE_SUPPRESSION_MODE)
                //对音频的噪声处理分为噪声抑制和瞬间尖波抑制两部分。其中噪声抑制可以调整抑制级别（ low、modeerate、high、veryhigh级别逐级增强）
                .setNoiseSuppressionLevel(RCRTCParamsType.NSLevel.parseValue(3))//RCRTCParamsType.NSLevel.parseValue(UserUtils.AUDIO_NOISE_SUPPRESSION_LEVEL)
                //是否支持高通滤波。， 默认 true
                .enableHighPassFilter(true)
                //设置增益控制开关，默认 true
                .enableAGCControl(true)
                .enableAGCLimiter(true)
                //设置声音信号量,取值范围（-3 - 31），默认值为 -3 ,设置声音目标数字信号量增益值。数字越大增益越小
                .setAGCTargetdbov(-3)
                //设置声音信号量电平压缩比，取值范围为 (0 - 90)， 默认值 为 9。值越大相对声音增益就越明显。取值范围为(0 - 90)， 默认值 为 9。与 AGCTargetdbov 配合使用
                .setAGCCompression(90)
                //采集音频信号放大开关，默认 true
                .enablePreAmplifier(true)
                //设置采集音频信号放大级别， 默认 1.0f
                .setPreAmplifierLevel(1.0f);
//        RCRTCAudioStreamConfig audioStreamConfig =  audioConfigBuilder.buildMusicMode() ;
        RCRTCAudioStreamConfig audioStreamConfig = audioConfigBuilder.buildDefaultMode();
        FinLog.d(TAG + "", "Audio --> enter");
        RCRTCEngine.getInstance().getDefaultAudioStream().setAudioConfig(audioStreamConfig);
        //视频流设置
        RCRTCVideoStreamConfig.Builder videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create();

        // 如果开启了镜像翻转 VideoFrame，则应关闭镜像预览功能，否则会造成2次翻转效果
        RCRTCEngine.getInstance().getDefaultVideoStream().setPreviewMirror(true);
        videoConfigBuilder.setVideoResolution(RCRTCParamsType.RCRTCVideoResolution.RESOLUTION_480_720).setVideoFps(RCRTCParamsType.RCRTCVideoFps.parseVideoFps(15));
        RCRTCEngine.getInstance().getDefaultVideoStream().enableTinyStream(true);
        RCRTCEngine.getInstance().getDefaultVideoStream().setCameraDisplayOrientation(0);
        RCRTCEngine.getInstance().getDefaultVideoStream().setFrameOrientation(-1);
        RCRTCEngine.getInstance().getDefaultVideoStream().setVideoConfig(videoConfigBuilder.build());
    }


    public void startCallActivity(boolean isAdmin) {
        Intent intent = new Intent(UserUtils.activity, CallActivity.class);
        intent.putExtra("EXTRA_IS_MASTER", isAdmin);
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
        initAudioManager();
        myUserId = RongIMClient.getInstance().getCurrentUserId();
        if (RCRTCEngine.getInstance().getRoom().getRemoteUsers().size() == 0) {
            adminUserId = myUserId;
        }
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
        loadSharing(true, "成功");
    }

    private boolean isOne = true;

    private void loadSharing(boolean isRefresh, String msg) {
        if (isOne) {//只响应一次
            isOne = false;
            onRongYunConnectionMonitoring.onLoadSharing(isRefresh, msg);
        }
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
        UserUtils.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UserUtils.activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
                // Create and audio manager that will take care of audio routing,
                // audio modes, audio device enumeration etc.
                audioManager = AppRTCAudioManager.create(UserUtils.activity.getApplicationContext(), new Runnable() {
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
        });
    }


    /**
     * 启动蓝牙
     */
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

    /**
     * 开始通话
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startCall() {
        try {
            room = RCRTCEngine.getInstance().getRoom();
            room.registerRoomListener(roomEventsListener);
            localUser = room.getLocalUser();

            RCRTCEngine.getInstance().getDefaultAudioStream().setAudioDataListener(audioDataListener);
            RCRTCEngine.getInstance().getDefaultAudioStream().setMicrophoneDisable(false);
            publishResource();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发布资源
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void publishResource() {
        if (localUser == null) {
            loadSharing(false, "不在房间里");
            return;
        }
        if (RongIMClient.getInstance().getCurrentConnectionStatus() == RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE) {
            String toastMsg = UserUtils.activity.getResources().getString(R.string.Thecurrentnetworkisnotavailable);
            loadSharing(false, toastMsg);
            return;
        }

        final List<RCRTCOutputStream> localAvStreams = new ArrayList<>();
        localAvStreams.add(RCRTCEngine.getInstance().getDefaultAudioStream());
        //   先发布本地流 音频   再发布共享屏幕视频流
        localUser.publishStreams(localAvStreams, new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {
                if (UserUtils.IS_BENDI) {
                    RCRTCVideoStreamConfig.Builder videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create();
                    videoConfigBuilder.setVideoResolution(RCRTCParamsType.RCRTCVideoResolution.RESOLUTION_720_1280);
                    videoConfigBuilder.setVideoFps(RCRTCParamsType.RCRTCVideoFps.Fps_10);
                    screenOutputStream = RCRTCEngine.getInstance()
                            .createVideoStream(RongRTCScreenCastHelper.VIDEO_TAG, videoConfigBuilder.build());
                    RCRTCVideoView videoView = new RCRTCVideoView(UserUtils.activity);
                    screenOutputStream.setVideoView(videoView);
                    screenCastHelper = new RongRTCScreenCastHelper();
                    if (Build.VERSION.SDK_INT > 28) { // 如果 SDK 版本大于28，需要启动一个前台service来录屏
                        Intent service = new Intent(UserUtils.activity, ScreenCastService.class);
                        serviceConnection = new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                                screenCastHelper.init(UserUtils.activity.getApplicationContext(), screenOutputStream, data, 720, 1280);
                                screenCastHelper.start();
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName componentName) {

                            }
                        };
                        UserUtils.activity.bindService(service, serviceConnection, BIND_AUTO_CREATE);
                    } else {
                        screenCastHelper.init(UserUtils.activity.getApplicationContext(), screenOutputStream, data, 720, 1280);
                        screenCastHelper.start();
                    }

                    localUser.publishStream(screenOutputStream, new IRCRTCResultCallback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailed(RTCErrorCode errorCode) {
                            UserUtils.activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (errorCode.equals(RTCErrorCode.RongRTCCodeHttpTimeoutError)) {
                                        publishResource();
                                    } else {
                                        loadSharing(false, errorCode.toString());
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {
                FinLog.e(TAG, "publish publish Failed()");
                // 50010 网络请求超时错误时，重试一次资源发布操作
                if (errorCode.equals(RTCErrorCode.RongRTCCodeHttpTimeoutError)) {
                    publishResource();
                } else {
                    loadSharing(false, errorCode.toString());
                }
            }
        });
    }

    /**
     * 订阅所有流
     */
    private void subscribeAll() {
        for (final RCRTCRemoteUser remoteUser : room.getRemoteUsers()) {
            if (remoteUser.getStreams().size() == 0) {
                continue;
            }
            localUser.subscribeStreams(remoteUser.getStreams(), new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    onRongYunConnectionMonitoring.onSuccessfullySubscribed();
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                    FinLog.d(TAG, "subscribeAll subscribeStreams userId = " + remoteUser.getUserId() + ", errorCode =" + errorCode.getValue());
                    // 50010 网络请求超时错误时，重试一次订阅操作
                    if (RTCErrorCode.RongRTCCodeHttpTimeoutError.equals(errorCode) && remoteUser.getStreams() != null && remoteUser.getStreams().size() > 0) {
                        localUser.subscribeStreams(remoteUser.getStreams(), null);
                    } else {
                        onRongYunConnectionMonitoring.onFailedSubscription(errorCode.toString());
                    }
                }
            });
        }
    }

    /**
     * 耳机连接状态监听
     */
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
    /**
     * 房间事件监听器
     */
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
                            onRongYunConnectionMonitoring.onReceiveMessage(toastMsg);
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
                                    onRongYunConnectionMonitoring.onReceiveMessage(toastMsg);
                                } else {
                                    String toastMsg = itemModel.name + " " + UserUtils.activity.getResources().getString(R.string.member_operate_admin_new);
                                    onRongYunConnectionMonitoring.onReceiveMessage(toastMsg);
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
                            intendToLeave(false, "您被踢出会议");
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
        }

        @Override
        public void onRemoteUserUnpublishResource(final RCRTCRemoteUser remoteUser, final List<RCRTCInputStream> streams) {
            FinLog.d(TAG, "onRemoteUserUnpublishResource remoteUser: " + remoteUser);
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
                    onRongYunConnectionMonitoring.onUserLeft(getUserName(remoteUser.getUserId()));
                    exitRoom(remoteUser.getUserId());
                }
            });
        }

        @Override
        public void onUserOffline(final RCRTCRemoteUser remoteUser) {
            UserUtils.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRongYunConnectionMonitoring.onUserOffline(getUserName(remoteUser.getUserId()));
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

    /**
     * 订阅流
     */
    private void alertRemotePublished(final RCRTCRemoteUser remoteUser) {
        Log.i(TAG, "alertRemotePublished()警报远程发布= start");
        localUser.subscribeStreams(remoteUser.getStreams(), new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: 订阅远程流成功");
                onRongYunConnectionMonitoring.onSuccessfullySubscribed();
            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {
                FinLog.d(TAG, "subscribeStreams userId = " + remoteUser.getUserId() + ", errorCode =" + errorCode.getValue());
                // 50010 网络请求超时错误时，重试一次订阅操作
                if (RTCErrorCode.RongRTCCodeHttpTimeoutError.equals(errorCode) && remoteUser.getStreams() != null &&
                        remoteUser.getStreams().size() > 0) {
                    localUser.subscribeStreams(remoteUser.getStreams(), null);
                } else {
                    onRongYunConnectionMonitoring.onFailedSubscription(errorCode.toString());
                }
            }
        });
    }

    /**
     * 获取用户名
     *
     * @param userId
     * @return
     */
    private String getUserName(String userId) {
        if (TextUtils.isEmpty(userId)) return userId;
        UserInfo userInfo = mMembersMap.get(userId);
        if (userInfo == null) return userId;
        return userInfo.userName;
    }

    /**
     * 获取身份
     *
     * @param mode
     * @return
     */
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

    /**
     * 取消屏幕投射
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void cancelScreenCast() {
        if (screenOutputStream == null || screenCastHelper != null) {
            return;
        }
        screenCastHelper.stop();
        screenCastHelper = null;
        localUser.unpublishStream(screenOutputStream, new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {
                screenOutputStream = null;
            }

            @Override
            public void onFailed(final RTCErrorCode errorCode) {
                screenOutputStream = null;
            }
        });
    }

    /**
     * 音频数据监听器
     */
    private IRCRTCAudioDataListener audioDataListener = new IRCRTCAudioDataListener() {
        @Override
        public byte[] onAudioFrame(RCRTCAudioFrame rtcAudioFrame) {
            return rtcAudioFrame.getBytes();
        }
    };

    /**
     * 整理房间成员
     */
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

    /**
     * 出口室
     *
     * @param userId
     */
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

    public void startAudioMixActivity(Activity activity) {
        activity.startActivity(new Intent(activity, AudioMixActivity.class));
    }
}