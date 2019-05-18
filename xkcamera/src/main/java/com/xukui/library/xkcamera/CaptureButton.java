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

import com.xukui.library.xkcamera.util.CheckPermission;

import static com.xukui.library.xkcamera.JCameraView.BUTTON_STATE_BOTH;
import static com.xukui.library.xkcamera.JCameraView.BUTTON_STATE_ONLY_CAPTURE;
import static com.xukui.library.xkcamera.JCameraView.BUTTON_STATE_ONLY_RECORDER;

public class CaptureButton extends View {

    public static final int STATE_IDLE = 0x001;//空闲状态
    public static final int STATE_PRESS = 0x002;//按下状态
    public static final int STATE_LONG_PRESS = 0x003;//长按状态
    public static final int STATE_RECORDERING = 0x004;//录制状态
    public static final int STATE_BAN = 0x005;//禁止状态

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
    private int mState;//当前按钮状态
    private int mActionState;//按钮可执行的功能状态（拍照,录制,两者）

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

    public CaptureButton(Context context, int buttonSize) {
        super(context);
        mSize = buttonSize;
        initData(context, null, 0);
        initView();
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(context, attrs, defStyleAttr);
        initView();
    }

    private void initData(Context context, AttributeSet attrs, int defStyleAttr) {
        mMaxDuration = 15000;
        mMinDuration = 3000;
        mProgressColor = 0xEE16AE16;
        mOutsideColor = 0xEEDCDCDC;
        mInsideColor = 0xFFFFFFFF;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CaptureButton, defStyleAttr, 0);
            mSize = a.getDimensionPixelOffset(R.styleable.CaptureButton_size, 200);
            mMaxDuration = a.getInteger(R.styleable.CaptureButton_max_duration, mMaxDuration);
            mMinDuration = a.getInteger(R.styleable.CaptureButton_min_duration, mMinDuration);
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

        mProgress = 0;
        mState = STATE_IDLE;//初始化为空闲状态
        mActionState = BUTTON_STATE_BOTH;//初始化按钮为可录制可拍照

        mTimer = new RecordCountDownTimer(mMaxDuration, mMaxDuration / 360);//定时器
    }

    private void initView() {

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mSize + mOutAddSize * 2, mSize + mOutAddSize * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setStyle(Paint.Style.FILL);

        mPaint.setColor(mOutsideColor); //外圆（半透明灰色）
        canvas.drawCircle(mCenterX, mCenterY, mOutRadius, mPaint);

        mPaint.setColor(mInsideColor);  //内圆（白色）
        canvas.drawCircle(mCenterX, mCenterY, mInRadius, mPaint);

        //如果状态为录制状态，则绘制录制进度条
        if (mState == STATE_RECORDERING) {
            mPaint.setColor(mProgressColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mStrokeWidth);
            canvas.drawArc(mRect, -90, mProgress, false, mPaint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                if (event.getPointerCount() > 1 || mState != STATE_IDLE)
                    break;
                mEventY = event.getY();     //记录Y值
                mState = STATE_PRESS;        //修改当前状态为点击按下

                //判断按钮状态是否为可录制状态
                if ((mActionState == BUTTON_STATE_ONLY_RECORDER || mActionState == BUTTON_STATE_BOTH))
                    postDelayed(mLongPressRunnable, 500);    //同时延长500启动长按后处理的逻辑Runnable
                break;

            case MotionEvent.ACTION_MOVE:
                if (mOnCaptureListener != null
                        && mState == STATE_RECORDERING
                        && (mActionState == BUTTON_STATE_ONLY_RECORDER || mActionState == BUTTON_STATE_BOTH)) {
                    //记录当前Y值与按下时候Y值的差值，调用缩放回调接口
                    mOnCaptureListener.onRecordZoom(mEventY - event.getY());
                }
                break;

            case MotionEvent.ACTION_UP:
                //根据当前按钮的状态进行相应的处理
                handlerUnpressByState();
                break;

            default:
                break;

        }
        return true;
    }

    //当手指松开按钮时候处理的逻辑
    private void handlerUnpressByState() {
        removeCallbacks(mLongPressRunnable); //移除长按逻辑的Runnable
        //根据当前状态处理
        switch (mState) {
            //当前是点击按下
            case STATE_PRESS:
                if (mOnCaptureListener != null && (mActionState == BUTTON_STATE_ONLY_CAPTURE || mActionState ==
                        BUTTON_STATE_BOTH)) {
                    startCaptureAnimation(mInRadius);
                } else {
                    mState = STATE_IDLE;
                }
                break;
            //当前是长按状态
            case STATE_RECORDERING:
                mTimer.cancel(); //停止计时器
                recordEnd();    //录制结束
                break;
        }
    }

    //录制结束
    private void recordEnd() {
        if (mOnCaptureListener != null) {
            if (mRecordedTime < mMinDuration)
                mOnCaptureListener.onRecordShort(mRecordedTime);//回调录制时间过短
            else
                mOnCaptureListener.onRecordEnd(mRecordedTime);  //回调录制结束
        }
        resetRecordAnim();  //重制按钮状态
    }

    //重制状态
    private void resetRecordAnim() {
        mState = STATE_BAN;
        mProgress = 0;//重制进度
        invalidate();
        //还原按钮初始状态动画
        startRecordAnimation(
                mOutRadius,
                mRadius,
                mInRadius,
                mRadius * 0.75f
        );
    }

    //内圆动画
    private void startCaptureAnimation(float inside_start) {
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_start * 0.75f, inside_start);
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mInRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        inside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //回调拍照接口
                mOnCaptureListener.onTakePicture();
                mState = STATE_BAN;
            }
        });
        inside_anim.setDuration(100);
        inside_anim.start();
    }

    //内外圆动画
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
        //当动画结束后启动录像Runnable并且回调录像开始接口
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //设置为录制状态
                if (mState == STATE_LONG_PRESS) {
                    if (mOnCaptureListener != null)
                        mOnCaptureListener.onRecordStart();
                    mState = STATE_RECORDERING;
                    mTimer.start();
                }
            }
        });
        set.playTogether(outside_anim, inside_anim);
        set.setDuration(100);
        set.start();
    }


    //更新进度条
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

    //长按线程
    private class LongPressRunnable implements Runnable {

        @Override
        public void run() {
            mState = STATE_LONG_PRESS;   //如果按下后经过500毫秒则会修改当前状态为长按状态

            //没有录制权限
            if (CheckPermission.getRecordState() != CheckPermission.STATE_SUCCESS) {
                mState = STATE_IDLE;
                if (mOnCaptureListener != null) {
                    mOnCaptureListener.onRecordError();
                    return;
                }
            }

            //启动按钮动画，外圆变大，内圆缩小
            startRecordAnimation(
                    mOutRadius,
                    mOutRadius + mOutAddSize,
                    mInRadius,
                    mInRadius - mInReduceSize
            );
        }

    }

    /**************************************************
     * 对外提供的API                     *
     **************************************************/

    //设置最长录制时间
    public void setMaxDuration(int duration) {
        mMaxDuration = duration;
        mTimer = new RecordCountDownTimer(duration, duration / 360);
    }

    //设置最短录制时间
    public void setMinDuration(int duration) {
        mMinDuration = duration;
    }

    //设置按钮功能（拍照和录像）
    public void setButtonFeatures(int state) {
        this.mActionState = state;
    }

    //是否空闲状态
    public boolean isIdle() {
        return mState == STATE_IDLE ? true : false;
    }

    //设置状态
    public void resetState() {
        mState = STATE_IDLE;
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

}
