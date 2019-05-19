package com.xukui.library.xkcamera.state;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.xukui.library.xkcamera.CameraInterface;
import com.xukui.library.xkcamera.ICameraView;

public class CameraMachine implements State {

    private Context mContext;
    private ICameraView mView;
    private State mPreviewState;       //浏览状态(空闲)
    private State mBorrowPictureState; //浏览图片
    private State mBorrowVideoState;   //浏览视频
    private State mState;

    public CameraMachine(Context context, ICameraView view) {
        mContext = context;
        mView = view;
        mPreviewState = new PreviewState(this);
        mBorrowPictureState = new BorrowPictureState(this);
        mBorrowVideoState = new BorrowVideoState(this);
        mState = mPreviewState;
    }

    public ICameraView getView() {
        return mView;
    }

    public Context getContext() {
        return mContext;
    }

    public void setState(State state) {
        mState = state;
    }

    //获取浏览图片状态
    State getBorrowPictureState() {
        return mBorrowPictureState;
    }

    //获取浏览视频状态
    State getBorrowVideoState() {
        return mBorrowVideoState;
    }

    //获取空闲状态
    State getPreviewState() {
        return mPreviewState;
    }

    @Override
    public void start(SurfaceHolder holder, float screenProp) {
        mState.start(holder, screenProp);
    }

    @Override
    public void stop() {
        mState.stop();
    }

    @Override
    public void focus(float x, float y, CameraInterface.OnFocusListener listener) {
        mState.focus(x, y, listener);
    }

    @Override
    public void switchCamera(SurfaceHolder holder, float screenProp) {
        mState.switchCamera(holder, screenProp);
    }

    @Override
    public void restart() {
        mState.restart();
    }

    @Override
    public void capture() {
        mState.capture();
    }

    @Override
    public void record(Surface surface, float screenProp) {
        mState.record(surface, screenProp);
    }

    @Override
    public void stopRecord(boolean isShort, long time) {
        mState.stopRecord(isShort, time);
    }

    @Override
    public void cancel(SurfaceHolder holder, float screenProp) {
        mState.cancel(holder, screenProp);
    }

    @Override
    public void confirm() {
        mState.confirm();
    }

    @Override
    public void zoom(float zoom, int type) {
        mState.zoom(zoom, type);
    }

    @Override
    public void flash(String mode) {
        mState.flash(mode);
    }

    public State getState() {
        return mState;
    }

}