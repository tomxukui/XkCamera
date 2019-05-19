package com.xukui.library.xkcamera.state;

import android.graphics.Bitmap;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.xukui.library.xkcamera.CameraInterface;
import com.xukui.library.xkcamera.CameraView;

class PreviewState implements State {

    private CameraMachine machine;

    PreviewState(CameraMachine machine) {
        this.machine = machine;
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
        if (machine.getView().handlerFocus(x, y)) {
            CameraInterface.getInstance().handleFocus(machine.getContext(), x, y, listener);
        }
    }

    @Override
    public void swtich(SurfaceHolder holder, float screenProp) {
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
                machine.getView().showPicture(bitmap, isVertical);
                machine.setState(machine.getBorrowPictureState());
            }

        });
    }

    @Override
    public void record(Surface surface, float screenProp) {
        CameraInterface.getInstance().startRecord(surface, screenProp);
    }

    @Override
    public void stopRecord(final boolean isShort, long time) {
        CameraInterface.getInstance().stopRecord(isShort, new CameraInterface.StopRecordCallback() {

            @Override
            public void recordResult(String url, Bitmap firstFrame) {
                if (isShort) {
                    machine.getView().resetState(CameraView.TYPE_SHORT);

                } else {
                    machine.getView().playVideo(firstFrame, url);
                    machine.setState(machine.getBorrowVideoState());
                }
            }

        });
    }

    @Override
    public void cancle(SurfaceHolder holder, float screenProp) {
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