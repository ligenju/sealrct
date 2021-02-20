package com.my.mylibrary;

import android.app.Activity;
import android.content.Context;
import android.media.projection.MediaProjectionManager;
import android.os.Build;

import androidx.annotation.RequiresApi;


import com.my.mylibrary.message.RoomInfoMessage;
import com.my.mylibrary.message.RoomKickOffMessage;
import com.my.mylibrary.utils.Utils;

import io.rong.imlib.RongIMClient;

/**
 * Description:
 * <p>
 * CreateTime：2021/02/20  17:03
 */
public class RongRTC {
    /**
     * 融云初始化 在application里
     *
     * @param context
     * @param appKey  融云appKey
     */
    public static void init(Context context, String appKey) {
        Utils.init(context);
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

    public static int MEDIA_PROJECTION_SERVICE_CODE = 101;

    /**
     * 获取共享屏幕权限
     * @param activity
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void getMediaProjectionService(Activity activity) {
        MediaProjectionManager manager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(manager.createScreenCaptureIntent(), MEDIA_PROJECTION_SERVICE_CODE);
    }

}