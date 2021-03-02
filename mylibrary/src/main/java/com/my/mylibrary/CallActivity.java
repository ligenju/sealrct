package com.my.mylibrary;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.my.mylibrary.base.RongRTCBaseActivity;
import com.my.mylibrary.bean.ItemModel;
import com.my.mylibrary.bean.UserInfo;
import com.my.mylibrary.call.AppRTCAudioManager;
import com.my.mylibrary.call.VideoViewManager;
import com.my.mylibrary.dialog.LoadDialog;
import com.my.mylibrary.dialog.PromptDialog;
import com.my.mylibrary.message.RoomInfoMessage;
import com.my.mylibrary.message.RoomKickOffMessage;
import com.my.mylibrary.screen_cast.RongRTCScreenCastHelper;
import com.my.mylibrary.screen_cast.ScreenCastService;
import com.my.mylibrary.utils.BluetoothUtil;
import com.my.mylibrary.utils.HeadsetPlugReceiver;
import com.my.mylibrary.utils.MirrorImageHelper;
import com.my.mylibrary.utils.OnHeadsetPlugListener;
import com.my.mylibrary.utils.RongRTCPopupWindow;
import com.my.mylibrary.utils.RongRTCTalkTypeUtil;
import com.my.mylibrary.utils.UserUtils;
import com.my.mylibrary.utils.Utils;
import com.my.mylibrary.watersign.TextureHelper;
import com.my.mylibrary.watersign.WaterMarkFilter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.rongcloud.rtc.api.RCRTCAudioMixer;
import cn.rongcloud.rtc.api.RCRTCEngine;
import cn.rongcloud.rtc.api.RCRTCLocalUser;
import cn.rongcloud.rtc.api.RCRTCRemoteUser;
import cn.rongcloud.rtc.api.RCRTCRoom;
import cn.rongcloud.rtc.api.callback.IRCRTCAudioDataListener;
import cn.rongcloud.rtc.api.callback.IRCRTCResultCallback;
import cn.rongcloud.rtc.api.callback.IRCRTCResultDataCallback;
import cn.rongcloud.rtc.api.callback.IRCRTCRoomEventsListener;
import cn.rongcloud.rtc.api.callback.IRCRTCStatusReportListener;
import cn.rongcloud.rtc.api.callback.IRCRTCVideoOutputFrameListener;
import cn.rongcloud.rtc.api.report.StatusBean;
import cn.rongcloud.rtc.api.report.StatusReport;
import cn.rongcloud.rtc.api.stream.RCRTCCameraOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCInputStream;
import cn.rongcloud.rtc.api.stream.RCRTCLiveInfo;
import cn.rongcloud.rtc.api.stream.RCRTCMicOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoInputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoOutputStream;
import cn.rongcloud.rtc.api.stream.RCRTCVideoStreamConfig;
import cn.rongcloud.rtc.api.stream.RCRTCVideoView;
import cn.rongcloud.rtc.base.RCRTCAudioFrame;
import cn.rongcloud.rtc.base.RCRTCMediaType;
import cn.rongcloud.rtc.base.RCRTCParamsType;
import cn.rongcloud.rtc.base.RCRTCResourceState;
import cn.rongcloud.rtc.base.RCRTCStream;
import cn.rongcloud.rtc.base.RCRTCVideoFrame;
import cn.rongcloud.rtc.base.RTCErrorCode;
import cn.rongcloud.rtc.core.CameraVideoCapturer;
import cn.rongcloud.rtc.utils.FinLog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.MessageContent;

import static io.rong.imlib.RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE;

public class CallActivity extends RongRTCBaseActivity implements View.OnClickListener, OnHeadsetPlugListener {
    private static String TAG = "CallActivity";
    private static final int SCREEN_CAPTURE_REQUEST_CODE = 10101;

    public static final String EXTRA_IS_MASTER = "EXTRA_IS_MASTER";

    // List of mandatory application unGrantedPermissions.
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
    private RCRTCVideoOutputStream screenOutputStream;
    private static RongRTCScreenCastHelper screenCastHelper;

    private AppRTCAudioManager audioManager = null;
    // Controls
    private VideoViewManager renderViewManager;
    private boolean isConnected = true;
    private ServiceConnection serviceConnection;
    private Button buttonHangUp;
    private LinearLayout waitingTips;
    private LinearLayout layoutNetworkStatusInfo;
    private TextView txtViewNetworkStatusInfo;
    private RelativeLayout mcall_more_container;
    private Handler handler = new Handler();
    private RongRTCPopupWindow popupWindow;
    private LinearLayout call_reder_container;
    private AppCompatCheckBox btnMuteSpeaker;
    private AppCompatCheckBox btnMuteMic;
    private ScrollView scrollView;
    private HorizontalScrollView horizontalScrollView;
    private RelativeLayout rel_sv; // sv父布局
    private List<ItemModel> mMembers = new ArrayList<>();
    private Map<String, UserInfo> mMembersMap = new HashMap<>();

    /**
     * true 关闭麦克风,false 打开麦克风
     */
    private boolean muteMicrophone = false;

    /**
     * true 关闭扬声器； false 打开扬声器
     */
    private boolean muteSpeaker = false;


    private String myUserId;
    // 管理员uerId,默认第一个加入房间的用户为管理员
    private String adminUserId;
    private boolean kicked = false;

    private RCRTCRoom room;
    private RCRTCLocalUser localUser;

    private HeadsetPlugReceiver headsetPlugReceiver = null;
    private boolean HeadsetPlugReceiverState = false; // false：开启音视频之前已经连接上耳机
    private WaterMarkFilter mWaterFilter;
    private boolean screenCastEnable = true;

    private List<StatusReport> statusReportList = new ArrayList<>();
    private SoundPool mSoundPool;
    private RCRTCLiveInfo liveInfo;
    List<String> unGrantedPermissions;
    private MirrorImageHelper mMirrorHelper;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HeadsetPlugReceiver.setOnHeadsetPlugListener(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        headsetPlugReceiver = new HeadsetPlugReceiver(BluetoothUtil.hasBluetoothA2dpConnected());
        registerReceiver(headsetPlugReceiver, intentFilter);

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_call);

        // Get Intent parameters.
        final Intent intent = getIntent();
        myUserId = RongIMClient.getInstance().getCurrentUserId();
        boolean admin = intent.getBooleanExtra(EXTRA_IS_MASTER, false);
        if (admin) {
            adminUserId = myUserId;
        }
        initAudioManager();
        initViews();
        checkPermissions();
        initBottomBtn();
        initRemoteScrollView();
        if (room == null) {
            return;
        }
        room.getRoomAttributes(null, new IRCRTCResultDataCallback<Map<String, String>>() {
            @Override
            public void onSuccess(final Map<String, String> data) {
                postUIThread(new Runnable() {
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
        initAudioMixing();
    }

    /**
     * 初始化音频管理器
     */
    private void initAudioManager() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(
                this.getApplicationContext(),
                new Runnable() {
                    // This method will be called each time the audio state (number and
                    // type of devices) has been changed.
                    @Override
                    public void run() {
                        onAudioManagerChangedState();
                    }
                });
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Initializing the audio manager...");
        audioManager.init();
    }

    private void initViews() {
        mcall_more_container = (RelativeLayout) findViewById(R.id.call_more_container);
        btnMuteSpeaker = (AppCompatCheckBox) findViewById(R.id.menu_mute_speaker);
        call_reder_container = (LinearLayout) findViewById(R.id.call_reder_container);
        buttonHangUp = (Button) findViewById(R.id.call_btn_hangup);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        horizontalScrollView = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
        btnMuteMic = (AppCompatCheckBox) findViewById(R.id.menu_mute_mic);
        waitingTips = (LinearLayout) findViewById(R.id.call_waiting_tips);
        layoutNetworkStatusInfo = (LinearLayout) findViewById(R.id.layout_network_status_tips);
        txtViewNetworkStatusInfo = (TextView) findViewById(R.id.textView_network_status_tips);
        rel_sv = (RelativeLayout) findViewById(R.id.rel_sv);
//        toggleCameraMicViewStatus();

        renderViewManager = new VideoViewManager();
        renderViewManager.setActivity(this);
        renderViewManager.setOnToggleListener(new VideoViewManager.OnToggleListener() {
            @Override
            public void onToggleTips(boolean isHasConnectedUser) {
                setWaitingTipsVisiable(isHasConnectedUser);
            }
        });
        buttonHangUp.setOnClickListener(this);
        btnMuteMic.setOnClickListener(this);
        btnMuteSpeaker.setOnClickListener(this);
        renderViewManager.setOnLocalVideoViewClickedListener(
                new VideoViewManager.OnLocalVideoViewClickedListener() {
                    @Override
                    public void onClick() {
                        toggleActionButtons(buttonHangUp.getVisibility() == View.VISIBLE);
                    }
                });

        if (UserUtils.IS_OBSERVER) {
            btnMuteMic.setChecked(true);
            btnMuteMic.setEnabled(false);
        }
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        unGrantedPermissions = new ArrayList();
        for (String permission : MANDATORY_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        }
        if (unGrantedPermissions.size() == 0) { // 已经获得了所有权限，开始加入聊天室
            startCall();
        } else { // 部分权限未获得，重新请求获取权限
            String[] array = new String[unGrantedPermissions.size()];
            ActivityCompat.requestPermissions(this, unGrantedPermissions.toArray(array), 0);
        }
    }

    /**
     * 在音频管理器更改状态
     */
    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    /**
     * 改变屏幕上除了视频通话之外的其他视图可见状态
     */
    private void toggleActionButtons(boolean isHidden) {
        if (isHidden) {
            buttonHangUp.setVisibility(View.GONE);
            mcall_more_container.setVisibility(View.GONE);
            btnMuteMic.setVisibility(View.GONE);
        } else {
            btnMuteMic.setVisibility(View.VISIBLE);
            buttonHangUp.setVisibility(View.VISIBLE);
            mcall_more_container.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 开始呼叫
     */
    private void startCall() {
        try {
            renderViewManager.initViews(this, UserUtils. IS_OBSERVER);
            room = RCRTCEngine.getInstance().getRoom();
            RCRTCEngine.getInstance().registerStatusReportListener(statusReportListener);
            room.registerRoomListener(roomEventsListener);
            localUser = room.getLocalUser();
            renderViewManager.setRongRTCRoom(room);
            RCRTCEngine.getInstance().getDefaultVideoStream().setVideoFrameListener(videoOutputFrameListener);
            RCRTCEngine.getInstance().getDefaultAudioStream().setAudioDataListener(audioDataListener);
            RCRTCEngine.getInstance().getDefaultAudioStream().setMicrophoneDisable(muteMicrophone);
            publishResource(); // 发布资源
            addAllVideoView();
            subscribeAll();

            if (!HeadsetPlugReceiverState) {
                int type = -1;
                if (BluetoothUtil.hasBluetoothA2dpConnected()) {
                    type = 0;
                } else if (BluetoothUtil.isWiredHeadsetOn(CallActivity.this)) {
                    type = 1;
                }
                if (type != -1) {
                    onNotifyHeadsetState(true, type);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发布音频流
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void publishResource() {
        if (UserUtils.IS_OBSERVER) {
            return;
        }
        if (localUser == null) {
            Toast.makeText(CallActivity.this, "不在房间里", Toast.LENGTH_SHORT).show();
            return;
        }

        if (RongIMClient.getInstance().getCurrentConnectionStatus() == NETWORK_UNAVAILABLE) {
            String toastMsg = getResources().getString(R.string.Thecurrentnetworkisnotavailable);
            Toast.makeText(CallActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
            return;
        }

        final List<RCRTCOutputStream> localAvStreams = new ArrayList<>();
        localAvStreams.add(RCRTCEngine.getInstance().getDefaultAudioStream());
        if (!UserUtils.IS_LIVE) {
            localUser.publishStreams(localAvStreams, new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {

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
            return;
        }
        if (UserUtils.IS_VIDEO_MUTE) {
            localUser.publishLiveStream(RCRTCEngine.getInstance().getDefaultAudioStream(), createLiveCallback());
        } else {
            localUser.publishDefaultLiveStreams(createLiveCallback());
        }

    }

    /**
     * 在“获取房间属性”处理程序上
     *
     * @param data
     */
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

                List<VideoViewManager.RenderHolder> holders = renderViewManager.getViewHolderByUserId(entry.getKey());
                for (VideoViewManager.RenderHolder holder : holders) {
                    if (TextUtils.equals(entry.getKey(), myUserId)) {
                        holder.updateUserInfo(getResources().getString(R.string.room_actor_me));
                    } else {
                        holder.updateUserInfo(model.name);
                    }
                }
                setWaitingTipsVisiable(mMembers.size() <= 1);
            }
            FinLog.d(TAG, "[MemberList] getRoomAttributes ==>  MemberSize=" + mMembers.size());
            sortRoomMembers();

        } catch (JSONException e) {
            e.printStackTrace();
        }
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
     * 视频输出帧监听器
     */
    private IRCRTCVideoOutputFrameListener videoOutputFrameListener = new IRCRTCVideoOutputFrameListener() {
        @Override
        public RCRTCVideoFrame processVideoFrame(RCRTCVideoFrame videoFrame) {
            boolean isTexture = videoFrame.getCaptureType() == RCRTCVideoFrame.CaptureType.TEXTURE;
            // TODO 水印目前仅支持 Texture 类型
            if (UserUtils.IS_WATER && isTexture) {
                videoFrame.setTextureId(
                        onDrawWater(videoFrame.getWidth(), videoFrame.getHeight(), videoFrame.getTextureId()));
            }
            onMirrorVideoFrame(videoFrame);
            return videoFrame;
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
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    alertRemotePublished(remoteUser);
                    updateResourceVideoView(remoteUser);
                }
            });

            room.getRoomAttributes(null, new IRCRTCResultDataCallback<Map<String, String>>() {
                @Override
                public void onSuccess(final Map<String, String> data) {
                    postUIThread(new Runnable() {
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

                                    List<VideoViewManager.RenderHolder> holders = renderViewManager.getViewHolderByUserId(entry.getKey());
                                    for (VideoViewManager.RenderHolder holder : holders) {
                                        if (TextUtils.equals(entry.getKey(), myUserId)) {
                                            holder.updateUserInfo(getResources().getString(R.string.room_actor_me));
                                        } else {
                                            holder.updateUserInfo(model.name);
                                        }
                                    }
                                    setWaitingTipsVisiable(mMembers.size() <= 1);
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
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    final PromptDialog dialog = PromptDialog.newInstance(CallActivity.this, getString(R.string.rtc_dialog_kicked_by_server));
                    dialog.setPromptButtonClickedListener(new PromptDialog.OnPromptButtonClickedListener() {
                        @Override
                        public void onPositiveButtonClicked() {
                            finish();
                        }

                        @Override
                        public void onNegativeButtonClicked() {
                            finish();
                        }
                    });
                    dialog.disableCancel();
                    dialog.setCancelable(false);
                    dialog.show();
                }
            });
        }

        @Override
        public void onVideoTrackAdd(final String userId, final String tag) {
            Log.i(TAG, "onVideoTrackAdd() userId: " + userId + " ,tag = " + tag);
            postUIThread(new Runnable() {
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
            postUIThread(new Runnable() {
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
                            String toastMsg = itemModel.name + " " + getResources().getString(R.string.rtc_join_room);
                            Toast.makeText(CallActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
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
                                    String toastMsg = getResources().getString(R.string.member_operate_admin_me);
                                    Toast.makeText(CallActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                } else {
                                    String toastMsg = itemModel.name + " " + getResources().getString(R.string.member_operate_admin_new);
                                    Toast.makeText(CallActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        UserInfo userInfo = new UserInfo();
                        userInfo.userId = roomInfoMessage.getUserId();
                        userInfo.userName = roomInfoMessage.getUserName();
                        userInfo.joinMode = roomInfoMessage.getJoinMode();
                        userInfo.timestamp = roomInfoMessage.getTimeStamp();
                        mMembersMap.put(roomInfoMessage.getUserId(), userInfo);

                        List<VideoViewManager.RenderHolder> holders = renderViewManager.getViewHolderByUserId(roomInfoMessage.getUserId());
                        for (VideoViewManager.RenderHolder holder : holders) {
                            if (!TextUtils.equals(roomInfoMessage.getUserId(), myUserId)) {
                                holder.updateUserInfo(roomInfoMessage.getUserName());
                            }
                            switch (roomInfoMessage.getJoinMode()) {
                                case RoomInfoMessage.JoinMode.AUDIO:
                                    holder.CameraSwitch(RongRTCTalkTypeUtil.C_CAMERA);
                                    break;
                                case RoomInfoMessage.JoinMode.AUDIO_VIDEO:
                                    holder.CameraSwitch(RongRTCTalkTypeUtil.O_CAMERA);
                                    break;
                                case RoomInfoMessage.JoinMode.OBSERVER:
                                    renderViewManager.removeVideoView(roomInfoMessage.getUserId());
                                    break;
                            }
                        }
                        if (mMembers.size() > 1) {
                            setWaitingTipsVisiable(false);
                        }
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
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    updateVideoView(remoteUser, stream, mute);
                }
            });

        }

        @Override
        public void onRemoteUserUnpublishResource(final RCRTCRemoteUser remoteUser, final List<RCRTCInputStream> streams) {
            FinLog.d(TAG, "onRemoteUserUnpublishResource remoteUser: " + remoteUser);
            if (streams == null) {
                return;
            }
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    for (RCRTCInputStream stream : streams) {
                        if (stream.getMediaType().equals(RCRTCMediaType.VIDEO)) {
                            renderViewManager.removeVideoView(false, remoteUser.getUserId(), stream.getTag());
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
            // Toast.makeText(CallActivity.this, remoteUser.getUserId() + " " +
            // getResources().getString(R.string.rtc_join_room), Toast.LENGTH_SHORT).show();

            //        if (!mMembersMap.containsKey(remoteUser.getUserId())) {//为兼容2.0版本加入房间不会触发room
            // info更新的情况，生成默认的ItemModel加入集合
            //            ItemModel itemModel = new ItemModel();
            //            itemModel.name = "";
            //            itemModel.mode = "0";
            //            itemModel.userId = remoteUser.getUserId();
            //            mMembers.add(0, itemModel);
            //        }
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    if (mMembers.size() > 1) {
                        setWaitingTipsVisiable(false);
                    }
                }
            });
            // renderViewManager.userJoin(remoteUser.getUserId(), remoteUser.getUserId(),
            // RongRTCTalkTypeUtil.O_CAMERA);
        }

        @Override
        public void onUserLeft(final RCRTCRemoteUser remoteUser) {
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CallActivity.this, getUserName(remoteUser.getUserId()) + " " + getResources().getString(R.string.rtc_quit_room), Toast.LENGTH_SHORT).show();
                    exitRoom(remoteUser.getUserId());
                    if (mMembers.size() <= 1) {
                        setWaitingTipsVisiable(true);
                    }
                }
            });
        }

        @Override
        public void onUserOffline(final RCRTCRemoteUser remoteUser) {
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CallActivity.this, getUserName(remoteUser.getUserId()) + " " + getResources().getString(R.string.rtc_user_offline), Toast.LENGTH_SHORT).show();
                    exitRoom(remoteUser.getUserId());
                    if (remoteUser.getUserId().equals(adminUserId)) {
                        adminUserId = null;
                    }
                    if (mMembers.size() <= 1) {
                        setWaitingTipsVisiable(true);
                    }
                }
            });
        }

        @Override
        public void onLeaveRoom(int reasonCode) {
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    final PromptDialog dialog = PromptDialog.newInstance(CallActivity.this, getString(R.string.rtc_status_im_error));
                    dialog.setPromptButtonClickedListener(new PromptDialog.OnPromptButtonClickedListener() {
                        @Override
                        public void onPositiveButtonClicked() {
                            finish();
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
     * 状态报告监听器
     */
    private IRCRTCStatusReportListener statusReportListener = new IRCRTCStatusReportListener() {
        @Override
        public void onAudioReceivedLevel(HashMap<String, String> audioLevel) {
            try {
                Iterator iter = audioLevel.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String key = "";
                    int val = 0;
                    if (null != entry.getKey()) {
                        key = entry.getKey().toString();
                    }
                    if (null != entry.getValue()) {
                        val = Integer.parseInt(entry.getValue().toString());
                    }
                    audiolevel(val, key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAudioInputLevel(String audioLevel) {
            if (localUser == null) return;
            int val = 0;
            try {
                val = TextUtils.isEmpty(audioLevel) ? 0 : Integer.parseInt(audioLevel);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (localUser.getDefaultAudioStream() != null && !TextUtils.isEmpty(localUser.getDefaultAudioStream().getStreamId())) {
                audiolevel(val, localUser.getDefaultAudioStream().getStreamId());
            }
        }

        @Override
        public void onConnectionStats(final StatusReport statusReport) {
            postUIThread(new Runnable() {
                @Override
                public void run() {
                    unstableNetworkToast(statusReport);
                    // 只有Debug模式下才显示详细的调试信息
                    if (renderViewManager == null || !BuildConfig.DEBUG) {
                        return;
                    }
                }
            });
        }
    };

    /**
     * 初始化音频混合
     */
    private void initAudioMixing() {
        AudioMixFragment.mixing = false;
        AudioMixFragment.mixMode = AudioMixFragment.MODE_PLAY_MIX;
        AudioMixFragment.audioPath = AudioMixFragment.DEFAULT_AUDIO_PATH;
        Arrays.fill(AudioEffectFragment.preloaded, false);
        AudioEffectFragment.loopCount = 1;
        RCRTCAudioMixer.getInstance().setMixingVolume(100);
        RCRTCAudioMixer.getInstance().setPlaybackVolume(100);
        RCRTCEngine.getInstance().getDefaultAudioStream().adjustRecordingVolume(100);
    }

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
     * 初始化底部按钮 默认竖屏
     */
    private void initBottomBtn() {

        ViewGroup.MarginLayoutParams mutelayoutParams =
                (ViewGroup.MarginLayoutParams) btnMuteMic.getLayoutParams();
        mutelayoutParams.setMargins(
                0, 0, dip2px(CallActivity.this, 50), dip2px(CallActivity.this, 16));
        btnMuteMic.setLayoutParams(mutelayoutParams);
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 设置等待提示可见
     *
     * @param visiable
     */
    public void setWaitingTipsVisiable(boolean visiable) {
        if (visiable) {
            visiable = !(mMembers != null && mMembers.size() > 1);
        }
        int tmp = waitingTips.getVisibility();
        if (visiable) {
            if (tmp != View.VISIBLE) {
                handler.removeCallbacks(timeRun);
            }
            waitingTips.setVisibility(View.VISIBLE);
        } else {
            waitingTips.setVisibility(View.GONE);
            if (tmp == View.VISIBLE) {
                handler.postDelayed(timeRun, 1000);
            }
        }
    }

    /**
     * 销毁弹出窗口
     */
    private void destroyPopupWindow() {
        if (null != popupWindow && popupWindow.isShowing()) {
            popupWindow.dismiss();
            popupWindow = null;
        }
    }


    public void onToggleMic(boolean mute) {
        muteMicrophone = mute;
        RCRTCEngine.getInstance().getDefaultAudioStream().setMicrophoneDisable(muteMicrophone);
//        RCRTCEngine.getInstance().getDefaultAudioStream().enableEarMonitoring(muteMicrophone);
    }

    /**
     * 添加所有视频视图
     */
    private void addAllVideoView() {
        for (RCRTCRemoteUser remoteUser : room.getRemoteUsers()) {
            addNewRemoteView(remoteUser); // 准备view
        }
    }

    private IRCRTCResultDataCallback createLiveCallback() {
        return new IRCRTCResultDataCallback<RCRTCLiveInfo>() {
            @Override
            public void onSuccess(RCRTCLiveInfo data) {
                //直播房间
                liveInfo = data;
                // TODO URL上传到服务器
                FinLog.d(TAG, "liveUrl::" + liveInfo.getLiveUrl());
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(LiveDataOperator.ROOM_ID, liveInfo.getRoomId());
                    jsonObject.put(LiveDataOperator.ROOM_NAME, liveInfo.getUserId());
                    jsonObject.put(LiveDataOperator.LIVE_URL, liveInfo.getLiveUrl());
                    jsonObject.put(LiveDataOperator.PUB_ID, liveInfo.getUserId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LiveDataOperator.getInstance().publish(jsonObject.toString(), new LiveDataOperator.OnResultCallBack() {
                    @Override
                    public void onSuccess(final String result) {
                        postUIThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("直播房间上传成功！" + result);
                            }
                        });
                    }

                    @Override
                    public void onFailed(final String error) {
                        postUIThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("直播房间上传失败！" + error);
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailed(final RTCErrorCode errorCode) {
                FinLog.e(TAG, "publish publish Failed()");
                postUIThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CallActivity.this, "发布资源失败 ：" + errorCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    private Runnable timeRun = new Runnable() {
        @Override
        public void run() {
            if (waitingTips != null && waitingTips.getVisibility() != View.VISIBLE) {
                handler.postDelayed(timeRun, 1000);
            }
        }
    };

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

    private String mapMode(int mode) {
        if (mode == RoomInfoMessage.JoinMode.AUDIO) {
            return getString(R.string.mode_audio);
        } else if (mode == RoomInfoMessage.JoinMode.AUDIO_VIDEO) {
            return getString(R.string.mode_audio_video);
        } else if (mode == RoomInfoMessage.JoinMode.OBSERVER) {
            return getString(R.string.mode_observer);
        }
        return "";
    }

    /**
     * 根据丢包率信息，提示弱网
     *
     * @param statusReport
     */
    private void unstableNetworkToast(StatusReport statusReport) {
        if (statusReportList != null && statusReportList.size() < 10) {
            statusReportList.add(statusReport);
            return;
        }

        Map<String, Map<String, Integer>> userStreamLostRate = new HashMap<>();

        for (StatusReport item : statusReportList) {
            // 统计本地发送音频流丢包率
            for (Map.Entry<String, StatusBean> entry : item.statusAudioSends.entrySet()) {
                String localUid = entry.getValue().uid;
                String streamId = entry.getValue().id;
                if (!userStreamLostRate.containsKey(localUid)) {
                    userStreamLostRate.put(localUid, new HashMap<String, Integer>());
                }
                if (entry.getValue().packetLostRate > 30) {
                    if (!userStreamLostRate.get(localUid).containsKey(streamId)) {
                        userStreamLostRate.get(localUid).put(streamId, 0);
                    }
                    userStreamLostRate
                            .get(localUid)
                            .put(streamId, userStreamLostRate.get(localUid).get(streamId) + 1);
                }
            }

            // 统计本地发送视频流丢包率
            for (Map.Entry<String, StatusBean> entry : item.statusVideoSends.entrySet()) {
                String localUid = entry.getValue().uid;
                String streamId = entry.getValue().id;
                if (!userStreamLostRate.containsKey(localUid)) {
                    userStreamLostRate.put(localUid, new HashMap<String, Integer>());
                }
                if (entry.getValue().packetLostRate > 15) {
                    if (!userStreamLostRate.get(localUid).containsKey(streamId)) {
                        userStreamLostRate.get(localUid).put(streamId, 0);
                    }
                    userStreamLostRate
                            .get(localUid)
                            .put(streamId, userStreamLostRate.get(localUid).get(streamId) + 1);
                }
            }

            // 统计远端音频流丢包率
            for (Map.Entry<String, StatusBean> entry : item.statusAudioRcvs.entrySet()) {
                String remoteUid = entry.getValue().uid;
                String streamId = entry.getValue().id;
                if (!userStreamLostRate.containsKey(remoteUid)) {
                    userStreamLostRate.put(remoteUid, new HashMap<String, Integer>());
                }
                if (entry.getValue().packetLostRate > 30) {
                    if (!userStreamLostRate.get(remoteUid).containsKey(streamId)) {
                        userStreamLostRate.get(remoteUid).put(streamId, 0);
                    }
                    userStreamLostRate
                            .get(remoteUid)
                            .put(streamId, userStreamLostRate.get(remoteUid).get(streamId) + 1);
                }
            }

            // 统计远端视频流丢包率
            for (Map.Entry<String, StatusBean> entry : item.statusVideoRcvs.entrySet()) {
                String remoteUid = entry.getValue().uid;
                String streamId = entry.getValue().id;
                if (!userStreamLostRate.containsKey(remoteUid)) {
                    userStreamLostRate.put(remoteUid, new HashMap<String, Integer>());
                }
                if (entry.getValue().packetLostRate > 15) {
                    if (!userStreamLostRate.get(remoteUid).containsKey(streamId)) {
                        userStreamLostRate.get(remoteUid).put(streamId, 0);
                    }
                    userStreamLostRate
                            .get(remoteUid)
                            .put(streamId, userStreamLostRate.get(remoteUid).get(streamId) + 1);
                }
            }
        }
        statusReportList.clear();

        String networkToast = "";
        boolean shouldToast = false;
        for (Map.Entry<String, Map<String, Integer>> entry : userStreamLostRate.entrySet()) {
            String userId = entry.getKey();
            for (Map.Entry<String, Integer> streamEntry : entry.getValue().entrySet()) {
                if (streamEntry.getValue() > 5) {
                    if (mMembersMap != null
                            && mMembersMap.containsKey(userId)
                            && !networkToast.contains(mMembersMap.get(userId).userName)) {
                        if (shouldToast) {
                            networkToast += ", ";
                        }
                        networkToast += mMembersMap.get(userId).userName;
                        shouldToast = true;
                    }
                }
            }
        }

        networkToast = String.format(getString(R.string.network_tip), networkToast);

        final boolean finalShouldToast = shouldToast;
        final String finalNetworkToast = networkToast;

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (finalShouldToast) {
                            layoutNetworkStatusInfo.setVisibility(View.VISIBLE);
                            txtViewNetworkStatusInfo.setText(finalNetworkToast);
                            txtViewNetworkStatusInfo.setVisibility(View.VISIBLE);

                            handler.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            layoutNetworkStatusInfo.setVisibility(View.GONE);
                                            txtViewNetworkStatusInfo.setVisibility(View.GONE);
                                        }
                                    },
                                    3000);
                        } else {
                            layoutNetworkStatusInfo.setVisibility(View.GONE);
                            txtViewNetworkStatusInfo.setVisibility(View.GONE);
                        }
                    }
                });
    }

    /**
     * 更新资源视频视图
     *
     * @param remoteUser
     */
    private void updateResourceVideoView(RCRTCRemoteUser remoteUser) {
        for (RCRTCInputStream rongRTCAVOutputStream : remoteUser.getStreams()) {
            RCRTCResourceState state = rongRTCAVOutputStream.getResourceState();
            if (rongRTCAVOutputStream.getMediaType() == RCRTCMediaType.VIDEO && renderViewManager != null) {
                FinLog.v(TAG, "updateResourceVideoView userId = " + remoteUser.getUserId() + " state = " + state);
                renderViewManager.updateTalkType(remoteUser.getUserId(), rongRTCAVOutputStream.getTag(),
                        state == RCRTCResourceState.DISABLED ? RongRTCTalkTypeUtil.C_CAMERA : RongRTCTalkTypeUtil.O_CAMERA);
            }
        }
    }

    /**
     * 更新视频视图
     *
     * @param remoteUser
     * @param rongRTCAVInputStream
     * @param enable
     */
    private void updateVideoView(RCRTCRemoteUser remoteUser, RCRTCInputStream rongRTCAVInputStream, boolean enable) {
        if (renderViewManager != null) {
            FinLog.v(TAG, "updateVideoView userId = " + remoteUser.getUserId() + " state = " + enable);
            renderViewManager.updateTalkType(remoteUser.getUserId(), rongRTCAVInputStream.getTag(),
                    enable ? RongRTCTalkTypeUtil.O_CAMERA : RongRTCTalkTypeUtil.C_CAMERA);
        }
    }

    /**
     * 警报远程发布
     *
     * @param remoteUser
     */
    private void alertRemotePublished(final RCRTCRemoteUser remoteUser) {
        Log.i(TAG, "alertRemotePublished() start");
        addNewRemoteView(remoteUser);
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

    /**
     * 全部订阅
     */
    private void subscribeAll() {
        for (final RCRTCRemoteUser remoteUser : room.getRemoteUsers()) {
            if (remoteUser.getStreams().size() == 0) {
                continue;
            }
            localUser.subscribeStreams(remoteUser.getStreams(), new IRCRTCResultCallback() {
                @Override
                public void onSuccess() {
                    postUIThread(new Runnable() {
                        @Override
                        public void run() {
                            updateResourceVideoView(remoteUser);
                        }
                    });
                }

                @Override
                public void onFailed(RTCErrorCode errorCode) {
                    FinLog.d(TAG, "subscribeAll subscribeStreams userId = " + remoteUser.getUserId() + ", errorCode =" + errorCode.getValue());
                    // 50010 网络请求超时错误时，重试一次订阅操作
                    if (RTCErrorCode.RongRTCCodeHttpTimeoutError.equals(errorCode) && remoteUser.getStreams() != null &&
                            remoteUser.getStreams().size() > 0) {
                        localUser.subscribeStreams(remoteUser.getStreams(), null);
                    }
                }
            });
        }
    }

    /**
     * 添加新的远程视图
     *
     * @param remoteUser
     */
    private void addNewRemoteView(RCRTCRemoteUser remoteUser) {
        List<RCRTCVideoInputStream> videoStreamList = new ArrayList<>();
        List<RCRTCInputStream> remoteAVStreams = remoteUser.getStreams();
        RCRTCInputStream audioStream = null;
        // 标记对方是否发布了摄像头视频流
        boolean cameraOpened = false;
        for (RCRTCInputStream inputStream : remoteAVStreams) {
            if (inputStream.getMediaType() == RCRTCMediaType.VIDEO) {
                videoStreamList.add((RCRTCVideoInputStream) inputStream);
                if (TextUtils.equals(inputStream.getTag(), RCRTCStream.RONG_TAG)) {
                    cameraOpened = true;
                }
            } else if (inputStream.getMediaType() == RCRTCMediaType.AUDIO) {
                // 只处理默认音频流，如果是自定义音频流，不做UI展示
                if (RCRTCStream.RONG_TAG.equals(inputStream.getTag())) {
                    audioStream = inputStream;
                }
            }
        }
        // 只有音频流，没有视频流时增加占位
        if (videoStreamList.isEmpty() && audioStream != null) {
            videoStreamList.add(null);
        }
        for (RCRTCVideoInputStream videoStream : videoStreamList) {
            UserInfo userInfo = mMembersMap.get(remoteUser.getUserId());
            String talkType = videoStream == null
                    ? RongRTCTalkTypeUtil.C_CAMERA
                    : RongRTCTalkTypeUtil.O_CAMERA;
            String userName = userInfo != null ? userInfo.userName : "";
            if (videoStream != null && videoStream.getVideoView() == null && !TextUtils.isEmpty(videoStream.getTag()) && TextUtils.equals(videoStream.getTag(), RongRTCScreenCastHelper.VIDEO_TAG)) {
                Log.d(TAG, "startCall: connectedUsers==2");
                renderViewManager.userJoin(remoteUser.getUserId(), videoStream.getTag(), userName, talkType);
                RCRTCVideoView remoteView = new RCRTCVideoView(this);
                renderViewManager.setVideoView(false, remoteUser.getUserId(),
                        videoStream.getTag(), remoteUser.getUserId(), remoteView, talkType);
                videoStream.setVideoView(remoteView);
            } else if (videoStream == null && UserUtils.IS_BENDI) {     //audio 占位
                Log.d(TAG, "startCall: connectedUsers==3");
                renderViewManager.userJoin(remoteUser.getUserId(), RCRTCStream.RONG_TAG, userName, talkType);
                RCRTCVideoView remoteView = new RCRTCVideoView(this);
                renderViewManager.setVideoView(false, remoteUser.getUserId(), RCRTCStream.RONG_TAG, remoteUser.getUserId(), remoteView, talkType);
            }
        }
    }

    private Runnable memoryRunnable = new Runnable() {
        @Override
        public void run() {
            getSystemMemory();
            if (handler != null) handler.postDelayed(memoryRunnable, 1000);
        }
    };

    /**
     * 获取系统内存
     */
    private void getSystemMemory() {
        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(info);
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        FinLog.e(TAG, "max Memory:" + Long.toString(maxMemory / (1024 * 1024)));
        FinLog.e(TAG, "free Memory:" + rt.freeMemory() / (1024 * 1024) + "m");
        FinLog.e(TAG, "total Memory:" + rt.totalMemory() / (1024 * 1024) + "m");
        FinLog.e(TAG, "系统是否处于低Memory运行：" + info.lowMemory);
        FinLog.e(TAG, "当系统剩余Memory低于" + (info.threshold >> 10) / 1024 + "m时就看成低内存运行");
    }

    /**
     * 提示
     *
     * @param message
     */
    private void showToastLengthLong(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 横竖屏检测
     *
     * @param config return true:横屏
     */
    private boolean screenCofig(Configuration config) {
        boolean screen = false; // 默认竖屏
        try {
            Configuration configuration = null;
            if (config == null) {
                configuration = this.getResources().getConfiguration();
            } else {
                configuration = config;
            }
            int ori = configuration.orientation;
            if (ori == configuration.ORIENTATION_LANDSCAPE) {
                screen = true;
            } else if (ori == configuration.ORIENTATION_PORTRAIT) {
                screen = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screen;
    }

    /**
     * 房间出口
     *
     * @param userId
     */
    private void exitRoom(String userId) {
        renderViewManager.delSelect(userId);
        renderViewManager.removeVideoView(userId);
        mMembersMap.remove(userId);
        for (int i = mMembers.size() - 1; i >= 0; --i) {
            ItemModel model = mMembers.get(i);
            if (TextUtils.equals(model.userId, userId)) {
                mMembers.remove(i);
                break;
            }
        }
    }

    /**
     * 音频电平
     *
     * @param val
     * @param key
     */
    private void audiolevel(final int val, final String key) {
        postUIThread(new Runnable() {
            @Override
            public void run() {
                if (null != renderViewManager && null != renderViewManager.connectedRemoteRenders && renderViewManager.getViewHolder(key) != null) {
                    VideoViewManager.RenderHolder renderHolder = renderViewManager.getViewHolder(key);
                    if (val > 0) {
                        if (key.equals(RongIMClient.getInstance().getCurrentUserId()) && muteMicrophone) {
                            renderHolder.coverView.closeAudioLevel();
                        } else {
                            renderHolder.coverView.showAudioLevel();
                        }
                    } else {
                        renderHolder.coverView.closeAudioLevel();
                    }
                }
            }
        });
    }

    /**
     * 第一次加入房间初始化远端的容器位置
     */
    private void initRemoteScrollView() {
        if (screenCofig(null)) {
            horizontalScreenViewInit();
        } else {
            verticalScreenViewInit();
        }
    }

    /**
     * 横屏View改变
     */
    private void horizontalScreenViewInit() {
        try {
            RelativeLayout.LayoutParams lp3 = (RelativeLayout.LayoutParams) rel_sv.getLayoutParams();
            lp3.addRule(RelativeLayout.BELOW, 0);

            WindowManager wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            int screenWidth = wm.getDefaultDisplay().getWidth();
            int screenHeight = wm.getDefaultDisplay().getHeight();
            int width = (screenHeight < screenWidth ? screenHeight : screenWidth) / 3;
            ViewGroup.MarginLayoutParams mutelayoutParams = (ViewGroup.MarginLayoutParams) btnMuteMic.getLayoutParams();
            mutelayoutParams.setMargins(0, 0, width, dip2px(CallActivity.this, 16));
            btnMuteMic.setLayoutParams(mutelayoutParams);
            if (null != horizontalScrollView) {
                if (horizontalScrollView.getChildCount() > 0) {
                    horizontalScrollView.removeAllViews();
                }
                horizontalScrollView.setVisibility(View.GONE);
            }
            if (null != scrollView) {
                if (scrollView.getChildCount() > 0) {
                    scrollView.removeAllViews();
                }
                scrollView.setVisibility(View.VISIBLE);
                call_reder_container.setOrientation(LinearLayout.VERTICAL);
                scrollView.addView(call_reder_container);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 竖屏View改变
     */
    private void verticalScreenViewInit() {
        initBottomBtn();
        if (null != scrollView) {
            if (scrollView.getChildCount() > 0) {
                scrollView.removeAllViews();
            }
            scrollView.setVisibility(View.GONE);
        }
        if (null != horizontalScrollView) {
            if (horizontalScrollView.getChildCount() > 0) {
                horizontalScrollView.removeAllViews();
            }
            horizontalScrollView.addView(call_reder_container);
            horizontalScrollView.setVisibility(View.VISIBLE);
            call_reder_container.setOrientation(LinearLayout.HORIZONTAL);
        }
    }

    /**
     * 要求屏幕投射
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void requestForScreenCast() {
        if (!screenCastEnable) {
            String toastInfo = getResources().getString(R.string.screen_cast_disabled);
            Toast.makeText(this, toastInfo, Toast.LENGTH_SHORT).show();
            return;
        }
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE);
    }

    /**
     * 准备离开当前房间
     *
     * @param initiative 是否主动退出，false为被踢的情况
     */
    private void intendToLeave(boolean initiative) {
        FinLog.i(TAG, "intendToLeave()-> " + initiative);
        cancelScreenCast(true);
        if (initiative) {
            selectAdmin();
        } else {
            kicked = true;
        }
        RCRTCAudioMixer.getInstance().stop();
        // 当前用户是观察者 或 离开房间时还有其他用户存在，直接退出
        if (screenOutputStream == null || !UserUtils.IS_BENDI) {
            disconnect();
        }
    }

    /**
     * 取消屏幕投射
     *
     * @param isHangup
     */
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
                postUIThread(new Runnable() {
                    @Override
                    public void run() {
                        renderViewManager.removeVideoView(true, myUserId, screenOutputStream.getTag());
                        screenOutputStream = null;
                        if (isHangup) {
                            disconnect();
                        }
                    }
                });
            }

            @Override
            public void onFailed(final RTCErrorCode errorCode) {
                postUIThread(new Runnable() {
                    @Override
                    public void run() {
                        renderViewManager.removeVideoView(true, myUserId, screenOutputStream.getTag());
                        screenOutputStream = null;
                        if (isHangup) {
                            disconnect();
                        }
                    }
                });
            }
        });
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
     * 断开房间
     */
    private void disconnect() {
        isConnected = false;
        LoadDialog.show(CallActivity.this);
        if (room != null) {
            room.deleteRoomAttributes(Collections.singletonList(myUserId), null, null);
        }
        RCRTCEngine.getInstance().leaveRoom(new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {
                postUIThread(new Runnable() {
                    @Override
                    public void run() {
                        FinLog.i(TAG, "quitRoom()->onUiSuccess");
                        if (!kicked) {
                            Toast.makeText(CallActivity.this, getResources().getString(R.string.quit_room_success), Toast.LENGTH_SHORT).show();
                        }
                        if (audioManager != null) {
                            audioManager.close();
                            audioManager = null;
                        }
                        LoadDialog.dismiss(CallActivity.this);
                        finish();
                    }
                });
            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {
                FinLog.i(TAG, "quitRoom()->onUiFailed : " + errorCode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (audioManager != null) {
                            audioManager.close();
                            audioManager = null;
                        }
                        LoadDialog.dismiss(CallActivity.this);
                        finish();
                    }
                });


            }
        });
    }

    /**
     * 启动蓝牙
     */
    private void startBluetoothSco() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            if (am.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
                am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
            am.setSpeakerphoneOn(false);
            am.startBluetoothSco();
        }
    }

    /**
     * 镜像翻转采集的视频数据
     *
     * @param rtcVideoFrame
     */
    private void onMirrorVideoFrame(RCRTCVideoFrame rtcVideoFrame) {
        boolean isFrontCamera = RCRTCEngine.getInstance().getDefaultVideoStream().isFrontCamera();
        if (!UserUtils.IS_MIRROR || mMirrorHelper == null || !isFrontCamera) {
            return;
        }
        long start = System.nanoTime();
        if (rtcVideoFrame.getCaptureType() == RCRTCVideoFrame.CaptureType.TEXTURE) {
            int newTextureId = mMirrorHelper.onMirrorImage(
                    rtcVideoFrame.getTextureId(), rtcVideoFrame.getWidth(), rtcVideoFrame.getHeight());
            rtcVideoFrame.setTextureId(newTextureId);
        } else {
            byte[] frameBytes = mMirrorHelper.onMirrorImage(
                    rtcVideoFrame.getData(), rtcVideoFrame.getWidth(), rtcVideoFrame.getHeight());
            rtcVideoFrame.setData(frameBytes);
        }
        Log.d(TAG, "onMirrorVideoFrame: " + (System.nanoTime() - start) * 1.0 / 1000000);
    }

    /**
     * 通知SCO音频状态更改
     *
     * @param scoAudioState
     */
    @Override
    public void onNotifySCOAudioStateChange(int scoAudioState) {
        switch (scoAudioState) {
            case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    am.setBluetoothScoOn(true);
                }
                break;
            case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                Log.d("onNotifyHeadsetState",
                        "onNotifySCOAudioStateChange: " + headsetPlugReceiver.isBluetoothConnected());
                if (headsetPlugReceiver.isBluetoothConnected()) {
                    startBluetoothSco();
                }
                break;
        }
    }

    /**
     * 通知耳机状态
     *
     * @param connected 是否连接
     * @param type      连接类型 ，0：蓝牙耳机；1：有线耳机
     */
    @Override
    public void onNotifyHeadsetState(boolean connected, int type) {
        try {
            if (connected) {
                HeadsetPlugReceiverState = true;
                if (type == 0) {
                    startBluetoothSco();
                }
                if (null != btnMuteSpeaker) {
                    btnMuteSpeaker.setBackgroundResource(R.drawable.img_capture_gray);
                    btnMuteSpeaker.setSelected(false);
                    btnMuteSpeaker.setEnabled(false);
                    btnMuteSpeaker.setClickable(false);
                    audioManager.onToggleSpeaker(false);
                }
            } else {
                if (type == 1 && BluetoothUtil.hasBluetoothA2dpConnected()) {
                    return;
                }
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    if (am.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
                        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    }
                    if (type == 0) {
                        am.stopBluetoothSco();
                        am.setBluetoothScoOn(false);
                        am.setSpeakerphoneOn(!muteSpeaker);
                    } else {
//                        RCRTCEngine.getInstance().enableSpeaker(!this.muteSpeaker);
                    }
                    audioManager.onToggleSpeaker(!muteSpeaker);
                }
                if (null != btnMuteSpeaker) {
                    btnMuteSpeaker.setBackgroundResource(R.drawable.selector_checkbox_capture);
                    btnMuteSpeaker.setSelected(false);
                    btnMuteSpeaker.setEnabled(true);
                    btnMuteSpeaker.setClickable(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 配置改变
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            popupWindow = null;
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            horizontalScreenViewInit();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            verticalScreenViewInit();
        }
        if (renderViewManager != null
                && null != unGrantedPermissions
                && unGrantedPermissions.size() == 0) {
            renderViewManager.rotateView();
        }
        if (mWaterFilter != null) {
            boolean isFrontCamera = RCRTCEngine.getInstance().getDefaultVideoStream().isFrontCamera();
            mWaterFilter.angleChange(isFrontCamera);
        }
    }

    /**
     * 添加水印
     *
     * @param width
     * @param height
     * @param textureID
     * @return
     */
    private int onDrawWater(int width, int height, int textureID) {
        boolean isFrontCamera = RCRTCEngine.getInstance().getDefaultVideoStream().isFrontCamera();
        if (mWaterFilter == null) {
            Bitmap logoBitmap = TextureHelper.loadBitmap(this, R.drawable.logo);
            mWaterFilter = new WaterMarkFilter(this, isFrontCamera, logoBitmap);
        }
        mWaterFilter.drawFrame(width, height, textureID, isFrontCamera);
        return mWaterFilter.getTextureID();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.call_btn_hangup) {
            FinLog.i(TAG, "intendToLeave()-> call_btn_hangup");
            intendToLeave(true);
        } else if (id == R.id.menu_mute_mic) {
            CheckBox checkBox = (CheckBox) v;
            FinLog.i(TAG, "isMute : " + checkBox.isChecked());
            onToggleMic(checkBox.isChecked());
        } else if (id == R.id.menu_mute_speaker) {
            CheckBox checkBox;// 为防止频繁快速点击造成音频卡顿，增加点击间隔限制
            if (Utils.isFastDoubleClick()) {
                showToast(R.string.rtc_processing);
                return;
            }
            destroyPopupWindow();
            checkBox = (CheckBox) v;
            this.muteSpeaker = checkBox.isChecked();
            if (muteSpeaker) {
                showToast(R.string.rtc_toast_switch_to_receiver);
            } else {
                showToast(R.string.rtc_toast_switch_to_speaker);
            }
            RCRTCEngine.getInstance().enableSpeaker(!this.muteSpeaker);
            audioManager.onToggleSpeaker(!muteSpeaker);
        } else if (id == R.id.call_waiting_tips) {
            toggleActionButtons(buttonHangUp.getVisibility() == View.VISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SCREEN_CAPTURE_REQUEST_CODE || resultCode != Activity.RESULT_OK) {
            return;
        }

        localUser.publishStream(screenOutputStream, new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {
                postUIThread(new Runnable() {
                    @Override
                    public void run() {
                        String toastInfo = getResources().getString(R.string.publish_failed);
                        Toast.makeText(CallActivity.this, toastInfo, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        RCRTCVideoStreamConfig.Builder videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create();
        videoConfigBuilder.setVideoResolution(RCRTCParamsType.RCRTCVideoResolution.RESOLUTION_720_1280);
        videoConfigBuilder.setVideoFps(RCRTCParamsType.RCRTCVideoFps.Fps_10);
        screenOutputStream = RCRTCEngine.getInstance()
                .createVideoStream(RongRTCScreenCastHelper.VIDEO_TAG, videoConfigBuilder.build());
        RCRTCVideoView videoView = new RCRTCVideoView(this);
        screenOutputStream.setVideoView(videoView);
        screenCastHelper = new RongRTCScreenCastHelper();
        if (Build.VERSION.SDK_INT > 28) { // 如果 SDK 版本大于28，需要启动一个前台service来录屏
            Intent service = new Intent(this, ScreenCastService.class);
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    screenCastHelper.init(getApplicationContext(), screenOutputStream, data, 720, 1280);
                    screenCastHelper.start();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            };
            bindService(service, serviceConnection, BIND_AUTO_CREATE);
        } else {
            screenCastHelper.init(getApplicationContext(), screenOutputStream, data, 720, 1280);
            screenCastHelper.start();
        }

        localUser.publishStream(screenOutputStream, new IRCRTCResultCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailed(RTCErrorCode errorCode) {
                postUIThread(new Runnable() {
                    @Override
                    public void run() {
                        String toastInfo = getResources().getString(R.string.publish_failed);
                        Toast.makeText(CallActivity.this, toastInfo, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        intendToLeave(true);
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        unGrantedPermissions.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                unGrantedPermissions.add(permissions[i]);
        }
        for (String permission : unGrantedPermissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                showToastLengthLong(getString(R.string.PermissionStr) + permission + getString(R.string.plsopenit));
                finish();
            } else ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        }
        if (unGrantedPermissions.size() == 0) {
            startCall();
        }
    }


    @Override
    protected void onDestroy() {
        destroyPopupWindow();
        HeadsetPlugReceiver.setOnHeadsetPlugListener(null);
        if (headsetPlugReceiver != null) {
            unregisterReceiver(headsetPlugReceiver);
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

        if (handler != null) {
            handler.removeCallbacks(memoryRunnable);
            handler.removeCallbacks(timeRun);
        }
        handler = null;
        super.onDestroy();

        if (mWaterFilter != null) {
            mWaterFilter.release();
        }
        if (mMirrorHelper != null) {
            mMirrorHelper.release();
        }
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
        mMirrorHelper = null;
        RCRTCMicOutputStream defaultAudioStream = RCRTCEngine.getInstance().getDefaultAudioStream();
        if (defaultAudioStream != null) {
            defaultAudioStream.setAudioDataListener(null);
        }

        RCRTCCameraOutputStream defaultVideoStream = RCRTCEngine.getInstance().getDefaultVideoStream();
        if (defaultVideoStream != null) {
            defaultVideoStream.setVideoFrameListener(null);
        }
        if (mSoundPool != null) {
            mSoundPool.release();
        }
        mSoundPool = null;
    }

}