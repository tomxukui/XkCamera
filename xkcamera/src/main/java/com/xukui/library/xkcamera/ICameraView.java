package com.xukui.library.xkcamera;

public interface ICameraView {

    void resetState(int type);

    void confirmState(int type);

    void showPhoto(byte[] bytes, boolean isVertical);

    void playVideo(String filePath, byte[] coverBytes);

    void stopVideo();

    boolean handlerFocus(float x, float y);

}