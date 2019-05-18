package com.xukui.library.xkcamera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.xukui.library.xkcamera.listener.CaptureListener;

public class CaptureLayout extends FrameLayout {

    private TextView tv_tip;
    private ImageView iv_back;
    private ImageView iv_cancel;
    private ImageView iv_confirm;
    private CaptureButton btn_capture;

    private OnItemClickListener mOnItemClickListener;

    private CaptureListener captureLisenter;    //拍照按钮监听

    public void setCaptureLisenter(CaptureListener captureLisenter) {
        this.captureLisenter = captureLisenter;
    }

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
        View view = LayoutInflater.from(context).inflate(R.layout.xkcamera_view_capture_layout, this);
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
        btn_capture.setCaptureLisenter(new CaptureListener() {

            @Override
            public void takePictures() {
                hideTipView();

                if (captureLisenter != null) {
                    captureLisenter.takePictures();
                }
            }

            @Override
            public void recordShort(long time) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(tv_tip, "alpha", 0f, 1f, 1f, 0f)
                        .setDuration(2500);
                animator.addListener(new Animator.AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animator) {
                        tv_tip.setText("录制时间过短");
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
                animator.start();

                if (captureLisenter != null) {
                    captureLisenter.recordShort(time);
                }
            }

            @Override
            public void recordStart() {
                hideTipView();

                if (captureLisenter != null) {
                    captureLisenter.recordStart();
                }
            }

            @Override
            public void recordEnd(long time) {
                startTypeBtnAnimator();

                if (captureLisenter != null) {
                    captureLisenter.recordEnd(time);
                }
            }

            @Override
            public void recordZoom(float zoom) {
                if (captureLisenter != null) {
                    captureLisenter.recordZoom(zoom);
                }
            }

            @Override
            public void recordError() {
                if (captureLisenter != null) {
                    captureLisenter.recordError();
                }
            }

        });
    }

    public void startTypeBtnAnimator() {
        iv_back.setVisibility(GONE);
        btn_capture.setVisibility(GONE);
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

    /**************************************************
     * 对外提供的API                      *
     **************************************************/
    public void resetCaptureLayout() {
        btn_capture.resetState();
        iv_cancel.setVisibility(GONE);
        iv_confirm.setVisibility(GONE);
        btn_capture.setVisibility(VISIBLE);
        iv_back.setVisibility(VISIBLE);
    }

    private void showTipView() {
        tv_tip.setText("轻触拍照, 长按摄像");

        ObjectAnimator.ofFloat(tv_tip, "alpha", 0f, 1f)
                .setDuration(500)
                .start();
    }

    private void hideTipView() {
        if (tv_tip.getAlpha() > 0) {
            ObjectAnimator.ofFloat(tv_tip, "alpha", 1f, 0f)
                    .setDuration(500)
                    .start();
        }
    }

    public void setDuration(int duration) {
        btn_capture.setDuration(duration);
    }

    public void setButtonFeatures(int state) {
        btn_capture.setButtonFeatures(state);
    }

    public void setTip(String tip) {
        tv_tip.setText(tip);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {

        void onBackClick();

        void onCancelClick();

        void onConfirmClick();

    }

}
