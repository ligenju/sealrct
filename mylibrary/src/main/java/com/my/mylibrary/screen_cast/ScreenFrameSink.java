package com.my.mylibrary.screen_cast;

public interface ScreenFrameSink {
    void onTexture(int textureWidth, int textureHeight, int oexTextureId, float[] transformMatrix, int rotation, long timestampNs);
}
