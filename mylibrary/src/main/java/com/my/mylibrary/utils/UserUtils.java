package com.my.mylibrary.utils;

import android.app.Activity;

/**
 * Description:
 * <p>
 * CreateTime：2021/02/10  15:50
 */
public class UserUtils {
    //融云APPKEY
    //    public static final String APP_KEY = "3argexb63fgue";
    public static final String APP_KEY = "pwe86ga5ps826";
    //token 连接融云
    public static String TOKEN = "";
    //房间ID=123456  建立房间
    public static String ROOMID = "123456";
    public static String USER_NAME = "123456";
    public static Activity activity;
    //设置音频视频
    public static boolean KEY_USE_AV_SETTING = true;
    //如果开启了镜像翻转 VideoFrame，则应关闭镜像预览功能，否则会造成2次翻转效果
    public static boolean IS_MIRROR = false;
    //显示水印
    public static boolean IS_WATER = false;
    //直播
    public static boolean IS_LIVE = false;
    // 进入房间时是否关闭摄像头
    public static boolean IS_VIDEO_MUTE = true;
    // 当前房间大于30人时，只能以观察者身份加入房间，不能发布音视频流，app层产品逻辑
    public static boolean IS_OBSERVER = false;
    //是否是共享屏幕
    public static boolean IS_BENDI = false;

    public static final String CUSTOM_FILE_TAG = "RongRTCFileVideo";


}