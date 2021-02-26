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
    //是音频音乐
    public static boolean IS_AUDIO_MUSIC = false;
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
    //自动化测试
    public static boolean IS_AUTO_TEST = false;
    //是否采用双声道
    public static boolean AUDIO_STEREO_ENABLE = false;
    //开启自定义音频加解密
    public static boolean AUDIO_ENCRYPTION = false;
    //开启自定义视频加解密
    public static boolean VIDEO_ENCRYPTION = false;
    //音频噪声高通滤波器
    public static boolean AUDIO_NOISE_HIGH_PASS_FILTER = true;
    //启用AUDIO回声过滤器
    public static boolean AUDIO_ECHO_CANCEL_FILTER_ENABLE = false;
    //音频AGC控制启用
    public static boolean AUDIO_AGC_CONTROL_ENABLE = true;
    //音频AGC限制器启用
    public static boolean AUDIO_AGC_LIMITER_ENABLE = true;
    //音频前置放大器启用
    public static boolean AUDIO_PRE_AMPLIFIER_ENABLE = true;
    //启用Tiny Stream
    public static boolean IS_STREAM_TINY = true;
    //视频编码配置
    public static boolean ENCODER_TYPE_KEY = true;
    //启用硬件编码器高级配置文件
    public static boolean ENCODER_LEVEL_KEY = false;
    //启用编码器纹理
    public static boolean ACQUISITION_MODE_KEY = true;
    //设置硬件编码器颜色
    public static int ENCODER_COLOR_FORMAT_VAL_KEY = 0;
    //视频解码配置
    public static boolean DECODER_TYPE_KEY = true;
    //设置硬件解码器颜色
    public static int DECODER_COLOR_FORMAT_VAL_KEY = 0;
    //音频传输比特率
    public static int AUDIO_TRANSPORT_BIT_RATE = 30;
    //设置音频采样率
    public static int AUDIO_SAMPLE_RATE = 48000;
    //设置麦克采集来源
    public static int AUDIO_SOURCE = 7;
    //AUDIO ECHO取消模式
    public static int AUDIO_ECHO_CANCEL_MODE = 0;
    //音频噪音抑制模式
    public static int AUDIO_NOISE_SUPPRESSION_MODE = 0;
    //音频噪音抑制等级
    public static int AUDIO_NOISE_SUPPRESSION_LEVEL = 1;
    //设置AGC Targetdbov
    public static int AUDIO_AGC_TARGET_DBOV = -3;
    //设置AGC压缩
    public static int AUDIO_AGC_COMPRESSION = 9;
    //捕捉相机显示方向
    public static int CAPTURE_CAMERA_DISPLAY_ORIENTATION_KEY = 0;
    //捕获帧方向
    public static int CAPTURE_FRAME_ORIENTATION_KEY = -1;
    //设置音频前置放大器级别
    public static float AUDIO_PRE_AMPLIFIER_LEVEL = 1.0f;
    //编码器比特率模式
    public static String ENCODER_BIT_RATE_MODE = "CBR";
    //解析度
    public static String RESOLUTION = "";
    //选择性框架
    public static String FPS = "";


    public static final String ENCODE_BIT_RATE_MODE_CQ = "CQ";
    public static final String ENCODE_BIT_RATE_MODE_VBR = "VBR";
    public static final String CUSTOM_FILE_TAG = "RongRTCFileVideo";


}