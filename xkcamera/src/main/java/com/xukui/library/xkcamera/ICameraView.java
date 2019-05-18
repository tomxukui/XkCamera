package com.xukui.library.xkcamera;

import android.graphics.Bitmap;

public interface ICameraView {

    void resetState(int type);

    void confirmState(int type);

    void showPicture(Bitmap bitmap, boolean isVertical);

    void playVideo(Bitmap firstFrame, String url);

    void stopVideo();

    boolean handlerFoucs(float x, float y);

}