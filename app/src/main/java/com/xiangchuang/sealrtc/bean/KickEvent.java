package com.xiangchuang.sealrtc.bean;

public class KickEvent {
    private String roomId;

    public KickEvent(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}
