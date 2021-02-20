package com.xiangchuang.sealrtc;

import android.app.Application;
import android.content.Context;

import com.xiangchuang.sealrtc.message.RoomInfoMessage;
import com.xiangchuang.sealrtc.message.RoomKickOffMessage;
import com.xiangchuang.sealrtc.utils.SessionManager;
import com.xiangchuang.sealrtc.utils.UserUtils;
import com.xiangchuang.sealrtc.utils.Utils;

import io.rong.imlib.RongIMClient;

/**
 * Description:
 * <p>
 * CreateTime：2021/02/10  15:24
 */
public class App extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        SessionManager.initContext(this);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        RongIMClient.init(this, UserUtils.APP_KEY,false);
        if (getApplicationInfo().packageName.equals(Utils.getCurProcessName(this))) {
            try {
                RongIMClient.registerMessageType(RoomInfoMessage.class);
                RongIMClient.registerMessageType(RoomKickOffMessage.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}