package com.xukui.library.xkcamera;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;

import com.xukui.library.xkcamera.listener.CaptureListener;
import com.xukui.library.xkcamera.listener.ClickListener;
import com.xukui.library.xkcamera.listener.JCameraListener;
import com.xukui.library.xkcamera.listener.OnErrorListener;
import com.xukui.library.xkcamera.listener.TypeListener;
import com.xukui.library.xkcamera.state.CameraMachine;
import com.xukui.library.xkcamera.util.FileUtil;
import com.xukui.library.xkcamera.view.CameraView;
import com.xukui.library.xkcamera.view.StableImageView;

import java.io.IOException;

public class JCameraView extends FrameLayout implements CameraInterface.CameraOpenOverCallback, SurfaceHolder.Callback, CameraView {

    //闪关灯状态
    private static final int TYPE_FLASH_AUTO = 0x021;
    private static final int TYPE_FLASH_ON = 0x022;
    private static final int TYPE_FLASH_OFF = 0x023;

    //拍照浏览时候的类型
    public static final int TYPE_PICTURE = 0x001;
    public static final int TYPE_VIDEO = 0x002;
    public static final int TYPE_SHORT = 0x003;
    public static final int TYPE_DEFAULT = 0x004;

    //录制视频比特率
    public static final int MEDIA_QUALITY_HIGH = 20 * 100000;
    public static final int MEDIA_QUALITY_MIDDLE = 16 * 100000;
    public static final int MEDIA_QUALITY_LOW = 12 * 100000;
    public static final int MEDIA_QUALITY_POOR = 8 * 100000;
    public static final int MEDIA_QUALITY_FUNNY = 4 * 100000;
    public static final int MEDIA_QUALITY_DESPAIR = 2 * 100000;
    public static final int MEDIA_QUALITY_SORRY = 1 * 80000;

    public static final int BUTTON_STATE_ONLY_CAPTURE = 0x101;      //只能拍照
    public static final int BUTTON_STATE_ONLY_RECORDER = 0x102;     //只能录像
    public static final int BUTTON_STATE_BOTH = 0x103;              //两者都可以

    private VideoView v_preview;
    private ImageView iv_photo;
    private StableImageView iv_flash;
    private StableImageView iv_switch;
    private CaptureLayout layout_capture;
    private FoucsView v_foucs;

    private CameraMachine mMachine;//Camera状态机
    private MediaPlayer mMediaPlayer;

    private Context mContext;

    private int mFlashType = TYPE_FLASH_OFF;
    private int mMaxDuration = 15000;//视频录制最长时间
    private float mScreenProp;//屏幕的比例(高/宽)
    private boolean mFirstTouch = true;
    private float mFirstTouchLength;
    private String mVideoUrl;//视频URL

    private Bitmap captureBitmap;//捕获的图片
    private Bitmap firstFrame;//第一帧图片

    //回调监听
    private OnErrorListener mOnErrorListener;

    private JCameraListener jCameraLisenter;
    private ClickListener leftClickListener;
    private ClickListener rightClickListener;

    public JCameraView(Context context) {
        this(context, null);
    }

    public JCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(context, attrs, defStyleAttr);
        initView(context);
        setView();
    }

    private void initData(Context context, AttributeSet attrs, int defStyleAttr) {
        mContext = context;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.JCameraView, defStyleAttr, 0);
            mMaxDuration = a.getInteger(R.styleable.JCameraView_max_duration, mMaxDuration);
            a.recycle();
        }

        mMachine = new CameraMachine(context, this, this);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.camera_view, this);

        v_preview = view.findViewById(R.id.v_preview);
        iv_photo = view.findViewById(R.id.iv_photo);
        iv_flash = view.findViewById(R.id.iv_flash);
        iv_switch = view.findViewById(R.id.iv_switch);
        layout_capture = view.findViewById(R.id.layout_capture);
        v_foucs = view.findViewById(R.id.v_foucs);
    }

    private void setView() {
        setWillNotDraw(false);

        //预览
        v_preview.getHolder().addCallback(this);

        //设置闪光灯
        setFlashView();
        iv_flash.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mFlashType++;

                if (mFlashType > TYPE_FLASH_OFF) {
                    mFlashType = TYPE_FLASH_AUTO;
                }

                setFlashView();
            }

        });

        //切换摄像头
        iv_switch.setOnClickListener(new StableImageView.OnClickListener() {

            @Override
            public void onClick(View view, int angle) {
                mMachine.swtich(v_preview.getHolder(), mScreenProp);
            }

        });

        //拍照、录像
        layout_capture.setDuration(mMaxDuration);
        layout_capture.setCaptureLisenter(new CaptureListener() {

            @Override
            public void takePictures() {
                iv_switch.setVisibility(INVISIBLE);
                iv_flash.setVisibility(INVISIBLE);
                mMachine.capture();
            }

            @Override
            public void recordStart() {
                iv_switch.setVisibility(INVISIBLE);
                iv_flash.setVisibility(INVISIBLE);
                mMachine.record(v_preview.getHolder().getSurface(), mScreenProp);
            }

            @Override
            public void recordShort(final long time) {
                layout_capture.setTextWithAnimation("录制时间过短");
                iv_switch.setVisibility(VISIBLE);
                iv_flash.setVisibility(VISIBLE);

                postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mMachine.stopRecord(true, time);
                    }

                }, 1500 - time);
            }

            @Override
            public void recordEnd(long time) {
                mMachine.stopRecord(false, time);
            }

            @Override
            public void recordZoom(float zoom) {
                mMachine.zoom(zoom, CameraInterface.TYPE_RECORDER);
            }

            @Override
            public void recordError() {
                if (mOnErrorListener != null) {
                    mOnErrorListener.onRecordError();
                }
            }

        });
        layout_capture.setTypeLisenter(new TypeListener() {

            @Override
            public void cancel() {
                mMachine.cancle(v_preview.getHolder(), mScreenProp);
            }

            @Override
            public void confirm() {
                mMachine.confirm();
            }

        });

        //退出
//        layout_capture.setReturnLisenter(new ReturnListener() {
//            @Override
//            public void onReturn() {
//                if (jCameraLisenter != null) {
//                    jCameraLisenter.quit();
//                }
//            }
//        });

        layout_capture.setLeftClickListener(new ClickListener() {

            @Override
            public void onClick() {
                if (leftClickListener != null) {
                    leftClickListener.onClick();
                }
            }

        });

        layout_capture.setRightClickListener(new ClickListener() {

            @Override
            public void onClick() {
                if (rightClickListener != null) {
                    rightClickListener.onClick();
                }
            }

        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mScreenProp == 0) {
            float widthSize = v_preview.getMeasuredWidth();
            float heightSize = v_preview.getMeasuredHeight();

            mScreenProp = heightSize / widthSize;
        }
    }

    @Override
    public void cameraHasOpened() {
        CameraInterface.getInstance().doStartPreview(v_preview.getHolder(), mScreenProp);
    }

    //生命周期onResume
    public void onResume() {
        resetState(TYPE_DEFAULT); //重置状态
        CameraInterface.getInstance().registerSensorManager(mContext);
        CameraInterface.getInstance().setSwitchView(iv_switch, iv_flash);
        mMachine.start(v_preview.getHolder(), mScreenProp);
    }

    //生命周期onPause
    public void onPause() {
        stopVideo();
        resetState(TYPE_PICTURE);
        CameraInterface.getInstance().isPreview(false);
        CameraInterface.getInstance().unregisterSensorManager(mContext);
    }

    //SurfaceView生命周期
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        new Thread() {

            @Override
            public void run() {
                CameraInterface.getInstance().doOpenCamera(JCameraView.this);
            }

        }.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CameraInterface.getInstance().doDestroyCamera();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN: {
                if (event.getPointerCount() == 1) {
                    setFocusViewWidthAnimation(event.getX(), event.getY());//显示对焦指示器
                }
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                if (event.getPointerCount() == 1) {
                    mFirstTouch = true;

                } else if (event.getPointerCount() == 2) {
                    //第一个点
                    float point_1_X = event.getX(0);
                    float point_1_Y = event.getY(0);

                    //第二个点
                    float point_2_X = event.getX(1);
                    float point_2_Y = event.getY(1);

                    float result = (float) Math.sqrt(Math.pow(point_1_X - point_2_X, 2) + Math.pow(point_1_Y - point_2_Y, 2));

                    if (mFirstTouch) {
                        mFirstTouchLength = result;
                        mFirstTouch = false;
                    }

                    if (((int) ((result - mFirstTouchLength) / (getWidth() / 16))) != 0) {
                        mFirstTouch = true;
                        mMachine.zoom(result - mFirstTouchLength, CameraInterface.TYPE_CAPTURE);
                    }
                }
            }
            break;

            case MotionEvent.ACTION_UP: {
                mFirstTouch = true;
            }
            break;

            default:
                break;

        }

        return true;
    }

    /**
     * 对焦框指示器动画
     */
    private void setFocusViewWidthAnimation(float x, float y) {
        mMachine.foucs(x, y, new CameraInterface.FocusCallback() {

            @Override
            public void focusSuccess() {
                v_foucs.setVisibility(INVISIBLE);
            }

        });
    }

    private void updateVideoViewSize(float videoWidth, float videoHeight) {
        if (videoWidth > videoHeight) {
            int height = (int) ((videoHeight / videoWidth) * getWidth());
            LayoutParams videoViewParam = new LayoutParams(LayoutParams.MATCH_PARENT, height);
            videoViewParam.gravity = Gravity.CENTER;
            v_preview.setLayoutParams(videoViewParam);
        }
    }

    /**************************************************
     * 对外提供的API                     *
     **************************************************/

    public void setSaveVideoPath(String path) {
        CameraInterface.getInstance().setSaveVideoPath(path);
    }


    public void setJCameraLisenter(JCameraListener jCameraLisenter) {
        this.jCameraLisenter = jCameraLisenter;
    }

    /**
     * 启动Camera错误回调
     */
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;

        CameraInterface.getInstance().setOnErrorListener(listener);
    }

    //设置CaptureButton功能（拍照和录像）
    public void setFeatures(int state) {
        this.layout_capture.setButtonFeatures(state);
    }

    //设置录制质量
    public void setMediaQuality(int quality) {
        CameraInterface.getInstance().setMediaQuality(quality);
    }

    @Override
    public void resetState(int type) {
        switch (type) {

            case TYPE_VIDEO: {
                stopVideo();//停止播放

                //初始化VideoView
                FileUtil.deleteFile(mVideoUrl);
                v_preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                mMachine.start(v_preview.getHolder(), mScreenProp);
            }
            break;

            case TYPE_PICTURE: {
                iv_photo.setVisibility(INVISIBLE);
            }
            break;

            case TYPE_SHORT:
                break;

            case TYPE_DEFAULT: {
                v_preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }
            break;

            default:
                break;
        }

        iv_switch.setVisibility(VISIBLE);
        iv_flash.setVisibility(VISIBLE);
        layout_capture.resetCaptureLayout();
    }

    @Override
    public void confirmState(int type) {
        switch (type) {

            case TYPE_VIDEO: {
                stopVideo();    //停止播放
                v_preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                mMachine.start(v_preview.getHolder(), mScreenProp);
                if (jCameraLisenter != null) {
                    jCameraLisenter.recordSuccess(mVideoUrl, firstFrame);
                }
            }
            break;

            case TYPE_PICTURE: {
                iv_photo.setVisibility(INVISIBLE);
                if (jCameraLisenter != null) {
                    jCameraLisenter.captureSuccess(captureBitmap);
                }
            }
            break;

            case TYPE_SHORT:
                break;

            case TYPE_DEFAULT:
                break;

            default:
                break;

        }

        layout_capture.resetCaptureLayout();
    }

    @Override
    public void showPicture(Bitmap bitmap, boolean isVertical) {
        if (isVertical) {
            iv_photo.setScaleType(ImageView.ScaleType.FIT_XY);

        } else {
            iv_photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        captureBitmap = bitmap;
        iv_photo.setImageBitmap(bitmap);
        iv_photo.setVisibility(VISIBLE);
        layout_capture.startAlphaAnimation();
        layout_capture.startTypeBtnAnimator();
    }

    @Override
    public void playVideo(Bitmap firstFrame, final String url) {
        mVideoUrl = url;
        JCameraView.this.firstFrame = firstFrame;

        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                try {
                    if (mMediaPlayer == null) {
                        mMediaPlayer = new MediaPlayer();
                    } else {
                        mMediaPlayer.reset();
                    }
                    mMediaPlayer.setDataSource(url);
                    mMediaPlayer.setSurface(v_preview.getHolder().getSurface());
                    mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer
                            .OnVideoSizeChangedListener() {
                        @Override
                        public void
                        onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                            updateVideoViewSize(mMediaPlayer.getVideoWidth(), mMediaPlayer
                                    .getVideoHeight());
                        }
                    });
                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mMediaPlayer.start();
                        }
                    });
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void stopVideo() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void setTip(String tip) {
        layout_capture.setTip(tip);
    }

    @Override
    public void startPreviewCallback() {
        handlerFoucs(v_foucs.getWidth() / 2, v_foucs.getHeight() / 2);
    }

    @Override
    public boolean handlerFoucs(float x, float y) {
        if (y > layout_capture.getTop()) {
            return false;
        }
        v_foucs.setVisibility(VISIBLE);
        if (x < v_foucs.getWidth() / 2) {
            x = v_foucs.getWidth() / 2;
        }
        int len = getWidth() - (v_foucs.getWidth() / 2);
        if (x > len) {
            x = len;
        }
        if (y < v_foucs.getWidth() / 2) {
            y = v_foucs.getWidth() / 2;
        }
        if (y > layout_capture.getTop() - v_foucs.getWidth() / 2) {
            y = layout_capture.getTop() - v_foucs.getWidth() / 2;
        }
        v_foucs.setX(x - v_foucs.getWidth() / 2);
        v_foucs.setY(y - v_foucs.getHeight() / 2);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v_foucs, "scaleX", 1, 0.6f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v_foucs, "scaleY", 1, 0.6f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(v_foucs, "alpha", 1f, 0.4f, 1f, 0.4f, 1f, 0.4f, 1f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(scaleX).with(scaleY).before(alpha);
        animSet.setDuration(400);
        animSet.start();
        return true;
    }

    public void setLeftClickListener(ClickListener clickListener) {
        this.leftClickListener = clickListener;
    }

    public void setRightClickListener(ClickListener clickListener) {
        this.rightClickListener = clickListener;
    }

    /**
     * 设置闪光灯
     */
    private void setFlashView() {
        switch (mFlashType) {

            case TYPE_FLASH_AUTO: {//自动闪光
                iv_flash.setImageResource(R.drawable.ic_flash_auto);
                mMachine.flash(Camera.Parameters.FLASH_MODE_AUTO);
            }
            break;

            case TYPE_FLASH_ON: {//开启闪光
                iv_flash.setImageResource(R.drawable.ic_flash_on);
                mMachine.flash(Camera.Parameters.FLASH_MODE_ON);
            }
            break;

            case TYPE_FLASH_OFF: {//关闭闪光
                iv_flash.setImageResource(R.drawable.ic_flash_off);
                mMachine.flash(Camera.Parameters.FLASH_MODE_OFF);
            }
            break;

            default:
                break;

        }
    }

}
