package com.my.mylibrary;

/**
 * Description:
 * <p>
 * CreateTime：2021/02/25  18:46
 */
public interface OnRongYunConnectionMonitoring {
    /**
     * TOKEN失效
     */
    void onTokenFail();

    /**
     * 连接融云失败
     */
    void onConnectionRongYunFailed(String err);

    /**
     * 连接房间成功
     * isAdmin= true== 只有一个人，下一个页面需要设置管理员id
     */
    void onConnectedToTheRoomSuccessfully(boolean isShare, boolean isAdmin);

    /**
     * 连接房间失败
     */
    void onFailedToConnectToRoom(String err);

    /**
     * 成功订阅音视频流
     */
    void onSuccessfullySubscribed();

    /**
     * 失败订阅音视频流
     */
    void onFailedSubscription(String err);

    /**
     * 加载共享服务 只响应一次
     *
     * @param isSuccess 是否加载成功
     * @param msg       失败原因
     */
    void onLoadSharing(boolean isSuccess, String msg);

    /**
     * 离线用户
     * name  用户名称
     */
    void onUserOffline(String name);

    /**
     * 离线用户
     * name  用户名称
     */
    void onUserLeft(String name);

    /**
     * 退出房间成功提示
     */
    void onSuccessfullyExitTheRoom(String prompt);

    /**
     * 收到消息
     */
    void onReceiveMessage(String message);

    /**
     * 销毁共享服务
     */
    void onDestroyed(String reason);

}