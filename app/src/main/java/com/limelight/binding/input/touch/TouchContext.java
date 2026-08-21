package com.limelight.binding.input.touch;

public interface TouchContext {
    int getActionIndex();
    void setPointerCount(int pointerCount);
    boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger);
    boolean touchMoveEvent(int eventX, int eventY, long eventTime);
    void touchUpEvent(int eventX, int eventY, long eventTime);
    void cancelTouch();
    boolean isCancelled();

    /** 设置鼠标长按模式 — true 时抑制长按触发右键，由悬浮窗的长按模式接管 */
    void setMouseLongPressActive(boolean active);
}
