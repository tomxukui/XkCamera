package com.xukui.library.xkcamera.state;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.xukui.library.xkcamera.CameraInterface;
import com.xukui.library.xkcamera.ICameraView;

public class CameraMachine implements State {

    private Context context;
    private State state;
    private ICameraView view;

    private State previewState;       //浏览状态(空闲)
    private State borrowPictureState; //浏览图片
    private State borrowVideoState;   //浏览视频

    public CameraMachine(Context context, ICameraView view) {
        this.context = context;
        previewState = new PreviewState(this);
        borrowPictureState = new BorrowPictureState(this);
        borrowVideoState = new BorrowVideoState(this);
        //默认设置为空闲状态
        this.state = previewState;
        this.view = view;
    }

    public ICameraView getView() {
        return view;
    }

    public Context getContext() {
        return context;
    }

    public void setState(State state) {
        this.state = state;
    }

    //获取浏览图片状态
    State getBorrowPictureState() {
        return borrowPictureState;
    }

    //获取浏览视频状态
    State getBorrowVideoState() {
        return borrowVideoState;
    }

    //获取空闲状态
    State getPreviewState() {
        return previewState;
    }

    @Override
    public void start(SurfaceHolder holder, float screenProp) {
        state.start(holder, screenProp);
    }

    @Override
    public void stop() {
        state.stop();
    }

    @Override
    public void focus(float x, float y, CameraInterface.OnFocusListener listener) {
        state.focus(x, y, listener);
    }

    @Override
    public void swtich(SurfaceHolder holder, float screenProp) {
        state.swtich(holder, screenProp);
    }

    @Override
    public void restart() {
        state.restart();
    }

    @Override
    public void capture() {
        state.capture();
    }

    @Override
    public void record(Surface surface, float screenProp) {
        state.record(surface, screenProp);
    }

    @Override
    public void stopRecord(boolean isShort, long time) {
        state.stopRecord(isShort, time);
    }

    @Override
    public void cancel(SurfaceHolder holder, float screenProp) {
        state.cancel(holder, screenProp);
    }

    @Override
    public void confirm() {
        state.confirm();
    }

    @Override
    public void zoom(float zoom, int type) {
        state.zoom(zoom, type);
    }

    @Override
    public void flash(String mode) {
        state.flash(mode);
    }

    public State getState() {
        return this.state;
    }

}