package com.xukui.library.xkcamera.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;

public class StableImageView extends AppCompatImageView implements SensorEventListener, View.OnClickListener {

    private SensorManager mSensorManager;

    private int mAngle;
    private OnClickListener mOnClickListener;

    public StableImageView(Context context) {
        this(context, null);
    }

    public StableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(context);
        initView();
    }

    private void initData(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    private void initView() {
        setOnClickListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mSensorManager != null) {
            Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (sensor != null) {
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (Sensor.TYPE_ACCELEROMETER != event.sensor.getType()) {
            return;
        }

        float[] values = event.values;
        int angle = getSensorAngle(values[0], values[1]);
        rotationAnimation(angle);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private int getSensorAngle(float x, float y) {
        if (Math.abs(x) > Math.abs(y)) {//横屏倾斜角度比较大
            if (x > 4) {//左边倾斜
                return 270;

            } else if (x < -4) {//右边倾斜
                return 90;

            } else {//倾斜角度不够大
                return 0;
            }

        } else {
            if (y > 7) {//左边倾斜
                return 0;

            } else if (y < -7) {//右边倾斜
                return 180;

            } else {//倾斜角度不够大
                return 0;
            }
        }
    }

    /**
     * 切换摄像头icon跟随手机角度进行旋转
     */
    private void rotationAnimation(int angle) {
        if (mAngle != angle) {
            int[] rotations = getRotations(angle);

            ObjectAnimator anim = ObjectAnimator.ofFloat(this, "rotation", rotations[0], rotations[1]);
            anim.setDuration(500);
            anim.start();

            mAngle = angle;
        }
    }

    private int[] getRotations(int angle) {
        int startRotaion = 0;
        int endRotation = 0;

        switch (mAngle) {

            case 0: {
                startRotaion = 0;

                if (angle == 90) {
                    endRotation = -90;

                } else if (angle == 270) {
                    endRotation = 90;
                }
            }
            break;

            case 90: {
                startRotaion = -90;

                if (angle == 0) {
                    endRotation = 0;

                } else if (angle == 180) {
                    endRotation = -180;
                }
            }
            break;

            case 180: {
                startRotaion = 180;

                if (angle == 90) {
                    endRotation = 270;

                } else if (angle == 270) {
                    endRotation = 90;
                }
            }
            break;

            case 270: {
                startRotaion = 90;

                if (angle == 0) {
                    endRotation = 0;

                } else if (angle == 180) {
                    endRotation = 180;
                }
            }
            break;

            default:
                break;

        }

        return new int[]{startRotaion, endRotation};
    }

    @Override
    public void onClick(View view) {
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(this, "scaleX", 0.8f, 1.2f, 1f);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(this, "scaleY", 0.8f, 1.2f, 1f);
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(this, "alpha", 0.6f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anim1, anim2, anim3);
        set.setInterpolator(new FastOutSlowInInterpolator());
        set.setDuration(200);
        set.start();

        if (mOnClickListener != null) {
            mOnClickListener.onClick(view, mAngle);
        }
    }

    public void setOnClickListener(@Nullable StableImageView.OnClickListener listener) {
        mOnClickListener = listener;
    }

    public interface OnClickListener {

        void onClick(View view, int angle);

    }

}