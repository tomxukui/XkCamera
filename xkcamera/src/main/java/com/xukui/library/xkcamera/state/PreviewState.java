package com.xukui.library.xkcamera.state;

import android.graphics.Bitmap;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.xukui.library.xkcamera.CameraInterface;
import com.xukui.library.xkcamera.CameraView;

class PreviewState implements State {

    private CameraMachine mMachine;

    PreviewState(CameraMachine machine) {
        mMachine = machine;
    }

    @Override
    public void start(SurfaceHolder holder, float screenProp) {
        CameraInterface.getInstance().doStartPreview(holder, screenProp);
    }

    @Override
    public void stop() {
        CameraInterface.getInstance().doStopPreview();
    }

    @Override
    public void focus(float x, float y, CameraInterface.OnFocusListener listener) {
        if (mMachine.getView().handlerFocus(x, y)) {
            CameraInterface.getInstance().handleFocus(mMachine.getScreenWidth(), mMachine.getScreenHeight(), x, y, listener);
        }
    }

    @Override
    public void switchCamera(SurfaceHolder holder, float screenProp) {
        CameraInterface.getInstance().switchCamera(holder, screenProp);
    }

    @Override
    public void restart() {
    }

    @Override
    public void capture() {
        CameraInterface.getInstance().takePicture(new CameraInterface.OnTakePictureListener() {

            @Override
            public void onResult(Bitmap bitmap, boolean isVertical) {
                mMachine.getView().showPhoto(bitmap, isVertical);
                mMachine.setState(mMachine.getBorrowPictureState());
            }

        });
    }

    @Override
    public void record(Surface surface, float screenProp) {
        CameraInterface.getInstance().startRecord(surface, screenProp);
    }

    @Override
    public void stopRecord(final boolean isShort, long time) {
        CameraInterface.getInstance().stopRecord(isShort, new CameraInterface.OnRecordListener() {

            @Override
            public void onShort() {
                mMachine.getView().resetState(CameraView.TYPE_SHORT);
            }

            @Override
            public void onResult(String url, Bitmap firstFrame) {
                mMachine.getView().playVideo(firstFrame, url);
                mMachine.setState(mMachine.getBorrowVideoState());
            }

        });
    }

    @Override
    public void cancel(SurfaceHolder holder, float screenProp) {
    }

    @Override
    public void confirm() {
    }

    @Override
    public void zoom(float zoom, int type) {
        CameraInterface.getInstance().setZoom(zoom, type);
    }

    @Override
    public void flash(String mode) {
        CameraInterface.getInstance().setFlashMode(mode);
    }

}