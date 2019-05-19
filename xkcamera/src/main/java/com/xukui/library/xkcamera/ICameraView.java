package com.xukui.library.xkcamera;

import android.graphics.Bitmap;

public interface ICameraView {

    void resetState(int type);

    void confirmState(int type);

    void showPhoto(byte[] bytes, boolean isVertical);

    void playVideo(Bitmap firstFrame, String url);

    void stopVideo();

    boolean handlerFocus(float x, float y);

}