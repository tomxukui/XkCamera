package com.xukui.library.xkcamera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CaptureLayout extends LinearLayout {

    private TextView tv_tip;
    private StableImageView iv_back;
    private ImageView iv_cancel;
    private ImageView iv_confirm;
    private CaptureButton btn_capture;

    private OnItemClickListener mOnItemClickListener;

    public CaptureLayout(Context context) {
        this(context, null);
    }

    public CaptureLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        setView();
    }

    private void initView(Context context) {
        setOrientation(VERTICAL);

        View view = LayoutInflater.from(context).inflate(R.layout.xkc_view_capture_layout, this);
        tv_tip = view.findViewById(R.id.tv_tip);
        iv_back = view.findViewById(R.id.iv_back);
        iv_cancel = view.findViewById(R.id.iv_cancel);
        iv_confirm = view.findViewById(R.id.iv_confirm);
        btn_capture = view.findViewById(R.id.btn_capture);
    }

    private void setView() {
        setWillNotDraw(false);

        //返回按钮
        iv_back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onBackClick();
                }
            }

        });

        //取消按钮
        iv_cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                showTipView();

                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onCancelClick();
                }
            }

        });

        //确认按钮
        iv_confirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                showTipView();

                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onConfirmClick();
                }
            }

        });

        //拍照按钮
        btn_capture.setOnCaptureListener(new CaptureButton.OnCaptureListener() {

            @Override
            public void onTakePicture() {
                hideTipView();

                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onTakePicture();
                }
            }

            @Override
            public void onRecordShort(long time) {
                shakeTipView();

                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onRecordShort(time);
                }
            }

            @Override
            public void onRecordStart() {
                hideTipView();

                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onRecordStart();
                }
            }

            @Override
            public void onRecordEnd(long time) {
                setCaptureEndView();

                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onRecordEnd(time);
                }
            }

            @Override
            public void onRecordZoom(float zoom) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onRecordZoom(zoom);
                }
            }

            @Override
            public void onRecordError() {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onRecordError();
                }
            }

        });
    }

    private void setCaptureStartView() {
        ObjectAnimator animator_cancel = ObjectAnimator.ofFloat(iv_cancel, "translationX", -getWidth() / 4, 0);
        ObjectAnimator animator_confirm = ObjectAnimator.ofFloat(iv_confirm, "translationX", getWidth() / 4, 0);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator_cancel, animator_confirm);
        set.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                iv_cancel.setClickable(false);
                iv_confirm.setClickable(false);

                iv_cancel.setVisibility(VISIBLE);
                iv_confirm.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                iv_cancel.setVisibility(GONE);
                iv_confirm.setVisibility(GONE);
                iv_back.setVisibility(VISIBLE);
                btn_capture.setVisibility(VISIBLE);
            }

        });
        set.setDuration(200);
        set.start();
    }

    public void setCaptureEndView() {
        iv_back.setVisibility(GONE);
        btn_capture.setVisibility(INVISIBLE);
        iv_cancel.setVisibility(VISIBLE);
        iv_confirm.setVisibility(VISIBLE);

        ObjectAnimator animator_cancel = ObjectAnimator.ofFloat(iv_cancel, "translationX", 0, -getWidth() / 4);
        ObjectAnimator animator_confirm = ObjectAnimator.ofFloat(iv_confirm, "translationX", 0, getWidth() / 4);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator_cancel, animator_confirm);
        set.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                iv_cancel.setClickable(false);
                iv_confirm.setClickable(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                iv_cancel.setClickable(true);
                iv_confirm.setClickable(true);
            }

        });
        set.setDuration(200);
        set.start();
    }

    public void reset() {
        if (btn_capture.getVisibility() == View.VISIBLE) {
            btn_capture.resetState();
            iv_cancel.setVisibility(GONE);
            iv_confirm.setVisibility(GONE);
            btn_capture.setVisibility(VISIBLE);
            iv_back.setVisibility(VISIBLE);

        } else {
            btn_capture.resetState();
            setCaptureStartView();
        }
    }

    private void showTipView() {
        tv_tip.clearAnimation();
        tv_tip.setText("轻触拍照, 长按摄像");
        tv_tip.setAlpha(1f);
    }

    private void hideTipView() {
        tv_tip.clearAnimation();
        tv_tip.setText("");
        tv_tip.setAlpha(0f);
    }

    private void shakeTipView() {
        tv_tip.clearAnimation();
        tv_tip.setText("录制时间过短");
        tv_tip.setAlpha(1f);

        ObjectAnimator anim1 = ObjectAnimator.ofFloat(tv_tip, "translationX", 0f, -20f, 20f, 0f);
        anim1.setRepeatCount(10);
        anim1.setDuration(20);

        ObjectAnimator anim2 = ObjectAnimator.ofFloat(tv_tip, "alpha", 1f, 0f);
        anim2.setDuration(300);
        anim2.setStartDelay(1000);

        AnimatorSet set = new AnimatorSet();
        set.play(anim1).before(anim2);
        set.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                showTipView();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

        });
        set.start();
    }

    public void setMaxDuration(int duration) {
        btn_capture.setMaxDuration(duration);
    }

    public void setMinDuration(int duration) {
        btn_capture.setMinDuration(duration);
    }

    public void setButtonFeatures(int state) {
        btn_capture.setButtonFeatures(state);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {

        void onBackClick();

        void onCancelClick();

        void onConfirmClick();

        void onTakePicture();

        void onRecordStart();

        void onRecordEnd(long time);

        void onRecordShort(long time);

        void onRecordError();

        void onRecordZoom(float zoom);

    }

}