package com.xukui.library.xkcamera.state;

import android.view.Surface;
import android.view.SurfaceHolder;

import com.xukui.library.xkcamera.CameraInterface;
import com.xukui.library.xkcamera.CameraView;

public class BorrowPictureState implements State {

    private CameraMachine mMachine;

    public BorrowPictureState(CameraMachine machine) {
        mMachine = machine;
    }

    @Override
    public void start(SurfaceHolder holder, float screenProp) {
        CameraInterface.getInstance().doStartPreview(holder, screenProp);
        mMachine.setState(mMachine.getPreviewState());
    }

    @Override
    public void stop() {
    }

    @Override
    public void focus(float x, float y, CameraInterface.OnFocusListener listener) {
    }

    @Override
    public void switchCamera(SurfaceHolder holder, float screenProp) {
    }

    @Override
    public void restart() {
    }

    @Override
    public void capture() {
    }

    @Override
    public void record(Surface surface, float screenProp) {
    }

    @Override
    public void stopRecord(boolean isShort, long time) {
    }

    @Override
    public void cancel(SurfaceHolder holder, float screenProp) {
        CameraInterface.getInstance().doStartPreview(holder, screenProp);
        mMachine.getView().resetState(CameraView.TYPE_PICTURE);
        mMachine.setState(mMachine.getPreviewState());
    }

    @Override
    public void confirm() {
        mMachine.getView().confirmState(CameraView.TYPE_PICTURE);
        mMachine.setState(mMachine.getPreviewState());
    }

    @Override
    public void zoom(float zoom, int type) {
    }

    @Override
    public void flash(String mode) {
    }

}