package com.my.mylibrary.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import cn.rongcloud.rtc.api.RCRTCEngine;
import cn.rongcloud.rtc.api.stream.RCRTCVideoView;

public class LocalVideoView extends RCRTCVideoView {

    // 正方形边长的一半
    private static final float RECT_RADIUS = 30;
    // 边框显示时长
    private static final long DISPLAY_TIME_MILLIS = 1000;
    private RectF rectF = new RectF();
    private Paint paint;
    private boolean enableReceiveTouchEvent = false;

    public LocalVideoView(Context context) {
        this(context, null);
    }

    public LocalVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(5);
    }

    public void enableReceiveTouchEvent(boolean enable) {
        this.enableReceiveTouchEvent = enable;
    }

    private void dismissRectDelayed(long timeMillis) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                rectF.setEmpty();
                postInvalidate();
            }
        }, timeMillis);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
        if (rectF.width() > 0) {
            canvas.drawRect(rectF, paint);
            dismissRectDelayed(DISPLAY_TIME_MILLIS);
        }
    }

    private boolean multiTouch = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enableReceiveTouchEvent) {
            return false;
        }
        if (event.getPointerCount() > 1) {
            multiTouch = true;
        } else if (MotionEvent.ACTION_UP == event.getAction()) {
            if (multiTouch) {
                multiTouch = false;
                return true;
            }
            float touchX = event.getX();
            float touchY = event.getY();
            int radius = convertDpToPixel(getContext(), RECT_RADIUS);
            rectF.left = touchX - radius;
            rectF.top = touchY - radius;
            rectF.right = touchX + radius;
            rectF.bottom = touchY + radius;
            RCRTCEngine.getInstance().getDefaultVideoStream().setCameraFocusPositionInPreview(touchX, touchY);
            RCRTCEngine.getInstance().getDefaultVideoStream().setCameraExposurePositionInPreview(touchX, touchY);
            postInvalidate();
        }
        return true;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param context Context to get resources and device specific display metrics
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into
     *     pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int convertDpToPixel(Context context, float dp) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
}
