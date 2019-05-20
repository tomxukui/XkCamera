package com.xukui.library.xkcamera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.xukui.library.xkcamera.bean.Mode;
import com.xukui.library.xkcamera.util.CheckPermission;

public class CaptureButton extends View {

    private int mSize;//按钮尺寸
    private float mRadius;//按钮半径
    private float mOutRadius;//外圆半径
    private float mInRadius;//内圆半径
    private float mStrokeWidth;//进度条宽度
    private int mOutAddSize;//长按外圆半径变大的Size
    private int mInReduceSize;//长按内圆缩小的Size

    private float mProgress;//录制视频的进度
    private int mMaxDuration;//录制视频最大时间长度
    private int mMinDuration;//最短录制时间限制

    private int mRecordedTime;//记录当前录制的时间
    private State mState;//当前按钮状态
    private Mode mMode;//当前模式

    private int mProgressColor;//进度条颜色
    private int mOutsideColor;//外圆背景色
    private int mInsideColor;//内圆背景色

    private float mCenterX;
    private float mCenterY;
    private float mEventY;

    private Paint mPaint;
    private RectF mRect;

    private RecordCountDownTimer mTimer;//计时器
    private LongPressRunnable mLongPressRunnable = new LongPressRunnable();//长按后处理的逻辑Runnable
    private OnCaptureListener mOnCaptureListener;

    public CaptureButton(Context context, int size) {
        super(context);
        mSize = size;
        initData(context, null, 0);
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(context, attrs, defStyleAttr);
    }

    private void initData(Context context, AttributeSet attrs, int defStyleAttr) {
        mProgress = 0;
        mMaxDuration = 10000;
        mMinDuration = 100;
        mRecordedTime = 0;
        mState = State.IDLE;
        mMode = Mode.ALL;
        mProgressColor = 0xEE16AE16;
        mOutsideColor = 0xEEDCDCDC;
        mInsideColor = 0xFFFFFFFF;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CaptureButton, defStyleAttr, 0);
            mSize = a.getDimensionPixelOffset(R.styleable.CaptureButton_size, 200);
            mProgressColor = a.getColor(R.styleable.CaptureButton_progress_color, mProgressColor);
            mOutsideColor = a.getColor(R.styleable.CaptureButton_outside_color, mOutsideColor);
            mInsideColor = a.getColor(R.styleable.CaptureButton_inside_color, mInsideColor);
            a.recycle();
        }

        mRadius = mSize / 2.0f;
        mOutRadius = mRadius;
        mInRadius = mRadius * 0.75f;
        mStrokeWidth = mSize / 15;
        mOutAddSize = mSize / 5;
        mInReduceSize = mSize / 8;
        mCenterX = (mSize + mOutAddSize * 2) / 2;
        mCenterY = (mSize + mOutAddSize * 2) / 2;

        mRect = new RectF(
                mCenterX - (mRadius + mOutAddSize - mStrokeWidth / 2),
                mCenterY - (mRadius + mOutAddSize - mStrokeWidth / 2),
                mCenterX + (mRadius + mOutAddSize - mStrokeWidth / 2),
                mCenterY + (mRadius + mOutAddSize - mStrokeWidth / 2));

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mTimer = new RecordCountDownTimer(mMaxDuration, mMaxDuration / 360);//定时器
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mSize + mOutAddSize * 2, mSize + mOutAddSize * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //外圆
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mOutsideColor);
        canvas.drawCircle(mCenterX, mCenterY, mOutRadius, mPaint);

        //内圆
        mPaint.setColor(mInsideColor);
        canvas.drawCircle(mCenterX, mCenterY, mInRadius, mPaint);

        //如果状态为录制状态，则绘制录制进度条
        if (mState == State.RECORDERING) {
            mPaint.setColor(mProgressColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mStrokeWidth);
            canvas.drawArc(mRect, -90, mProgress, false, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN: {//按下
                if (event.getPointerCount() == 1 && mState == State.IDLE) {
                    mEventY = event.getY();
                    mState = State.PRESS;

                    //判断按钮状态是否为可录制状态
                    if (mMode == Mode.VIDEO || mMode == Mode.ALL) {
                        postDelayed(mLongPressRunnable, 500);
                    }
                }
            }
            break;

            case MotionEvent.ACTION_MOVE: {//移动
                if (mState == State.RECORDERING && (mMode == Mode.VIDEO || mMode == Mode.ALL)) {
                    if (mOnCaptureListener != null) {
                        mOnCaptureListener.onRecordZoom(mEventY - event.getY());
                    }
                }
            }
            break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {//抬起
                handlerUnpressByState();
            }
            break;

            default:
                break;

        }

        return true;
    }

    /**
     * 当手指松开按钮时候处理的逻辑
     */
    private void handlerUnpressByState() {
        //移除长按逻辑的Runnable
        removeCallbacks(mLongPressRunnable);

        switch (mState) {

            case PRESS: {//当前是点击按下
                if (mMode == Mode.PHOTO || mMode == Mode.ALL) {
                    startCaptureAnimation();

                } else {
                    mState = State.IDLE;
                }
            }
            break;

            case RECORDERING: {//当前是长按状态
                //停止计时器
                mTimer.cancel();

                //录制结束
                recordEnd();
            }
            break;

            default:
                break;

        }
    }

    /**
     * 录制结束
     */
    private void recordEnd() {
        if (mOnCaptureListener != null) {
            if (mRecordedTime < mMinDuration) {//回调录制时间过短
                mOnCaptureListener.onRecordShort(mRecordedTime);

            } else {//回调录制结束
                mOnCaptureListener.onRecordEnd(mRecordedTime);
            }
        }

        //重制按钮状态
        resetRecord();
    }

    /**
     * 重制状态
     */
    private void resetRecord() {
        mState = State.BAN;
        mProgress = 0;

        invalidate();

        //还原按钮初始状态动画
        startRecordAnimation(mOutRadius, mRadius, mInRadius, mRadius * 0.75f);
    }

    /**
     * 内圆动画
     */
    private void startCaptureAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(mInRadius, mInRadius * 0.75f, mInRadius).setDuration(100);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mInRadius = (float) animation.getAnimatedValue();

                invalidate();
            }

        });
        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mState = State.BAN;

                if (mOnCaptureListener != null) {
                    mOnCaptureListener.onTakePicture();
                }
            }

        });
        animator.start();
    }

    /**
     * 内外圆动画
     */
    private void startRecordAnimation(float outside_start, float outside_end, float inside_start, float inside_end) {
        ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);

        //外圆动画监听
        outside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mOutRadius = (float) animation.getAnimatedValue();
                invalidate();
            }

        });

        //内圆动画监听
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mInRadius = (float) animation.getAnimatedValue();
                invalidate();
            }

        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(outside_anim, inside_anim);
        set.setDuration(100);
        set.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //设置为录制状态
                if (mState == State.LONG_PRESS) {
                    mState = State.RECORDERING;
                    mTimer.start();

                    if (mOnCaptureListener != null) {
                        mOnCaptureListener.onRecordStart();
                    }
                }
            }

        });
        set.start();
    }

    /**
     * 更新进度条
     */
    private void updateProgress(long millisUntilFinished) {
        mRecordedTime = (int) (mMaxDuration - millisUntilFinished);
        mProgress = 360f - millisUntilFinished / (float) mMaxDuration * 360f;

        invalidate();
    }

    /**
     * 录制视频计时器
     */
    private class RecordCountDownTimer extends CountDownTimer {

        RecordCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            updateProgress(millisUntilFinished);
        }

        @Override
        public void onFinish() {
            updateProgress(0);
            recordEnd();
        }

    }

    /**
     * 长按线程
     */
    private class LongPressRunnable implements Runnable {

        @Override
        public void run() {
            mState = State.LONG_PRESS;//如果按下后经过500毫秒则会修改当前状态为长按状态

            //没有录制权限
            if (CheckPermission.getRecordState() != CheckPermission.STATE_SUCCESS) {
                mState = State.IDLE;

                if (mOnCaptureListener != null) {
                    mOnCaptureListener.onRecordError();
                    return;
                }
            }

            //启动按钮动画，外圆变大，内圆缩小
            startRecordAnimation(mOutRadius, mOutRadius + mOutAddSize, mInRadius, mInRadius - mInReduceSize);
        }

    }

    /**
     * 设置最长录制时间
     */
    public void setMaxDuration(int duration) {
        mMaxDuration = duration;
        mTimer = new RecordCountDownTimer(duration, duration / 360);
    }

    /**
     * 设置最短录制时间
     */
    public void setMinDuration(int duration) {
        mMinDuration = duration;
    }

    /**
     * 设置模式
     */
    public void setMode(Mode mode) {
        mMode = mode;
    }

    /**
     * 是否空闲状态
     */
    public boolean isIdle() {
        return mState == State.IDLE ? true : false;
    }

    /**
     * 设置状态
     */
    public void resetState() {
        mState = State.IDLE;
    }

    public void setOnCaptureListener(OnCaptureListener listener) {
        mOnCaptureListener = listener;
    }

    public interface OnCaptureListener {

        void onTakePicture();

        void onRecordShort(long time);

        void onRecordStart();

        void onRecordEnd(long time);

        void onRecordZoom(float zoom);

        void onRecordError();

    }

    /**
     * 按钮状态
     */
    private enum State {

        IDLE,//空闲状态
        PRESS,//按下状态
        LONG_PRESS,//长按状态
        RECORDERING,//录制状态
        BAN//禁止状态

    }

}