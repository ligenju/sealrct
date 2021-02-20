package com.xiangchuang.sealrtc.http.callbacks;

public interface HttpCallback<T> {

    void onSuccess(T t);

    void onFail(HttpErrorCode errorCode);
}
