package com.xukui.library.xkcamera.view;

import android.graphics.Bitmap;

public interface CameraView {

    void resetState(int type);

    void confirmState(int type);

    void showPicture(Bitmap bitmap, boolean isVertical);

    void playVideo(Bitmap firstFrame, String url);

    void stopVideo();

    boolean handlerFoucs(float x, float y);

}