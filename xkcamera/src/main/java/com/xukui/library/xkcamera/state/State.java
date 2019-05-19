package com.xukui.library.xkcamera.state;

import android.view.Surface;
import android.view.SurfaceHolder;

import com.xukui.library.xkcamera.CameraInterface;

public interface State {

    void start(SurfaceHolder holder, float screenProp);

    void stop();

    void focus(float x, float y, CameraInterface.OnFocusListener listener);

    void swtich(SurfaceHolder holder, float screenProp);

    void restart();

    void capture();

    void record(Surface surface, float screenProp);

    void stopRecord(boolean isShort, long time);

    void cancel(SurfaceHolder holder, float screenProp);

    void confirm();

    void zoom(float zoom, int type);

    void flash(String mode);

}