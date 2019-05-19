package com.xukui.library.xkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.xukui.library.xkcamera.util.AngleUtil;
import com.xukui.library.xkcamera.util.CameraParamUtil;
import com.xukui.library.xkcamera.util.CheckPermission;
import com.xukui.library.xkcamera.util.DeviceUtil;
import com.xukui.library.xkcamera.util.FileUtil;
import com.xukui.library.xkcamera.util.ImageUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.Bitmap.createBitmap;

public class CameraInterface implements Camera.PreviewCallback {

    private volatile static CameraInterface mCameraInterface;

    private Camera mCamera;
    private boolean mIsPreviewing = false;
    private boolean mIsRecorder = false;//当前是否在录像

    private int CAMERA_FRONT_POSITION = -1;//正面摄像头
    private int CAMERA_BACK_POSITION = -1;//反面摄像头
    private int mSelectedCamera = -1;//当前选择的摄像头

    private MediaRecorder mMediaRecorder;
    private String mVideoDirectory;
    private String mVideoFileName;
    private byte[] mVideoCoverBytes;

    private OnErrorListener mOnErrorListener;

    private int preview_width;
    private int preview_height;

    private int angle = 0;
    private int cameraAngle = 90;//摄像头角度   默认为90度
    private byte[] firstFrame_data;

    public static final int TYPE_RECORDER = 0x090;
    public static final int TYPE_CAPTURE = 0x091;
    private int nowScaleRate = 0;
    private int recordScleRate = 0;

    //视频质量
    private int mediaQuality = CameraView.MEDIA_QUALITY_MIDDLE;

    private SensorManager mSensorManager = null;

    //获取CameraInterface单例
    public static synchronized CameraInterface getInstance() {
        if (mCameraInterface == null) {
            synchronized (CameraInterface.class) {
                if (mCameraInterface == null) {
                    mCameraInterface = new CameraInterface();
                }
            }
        }

        return mCameraInterface;
    }

    private CameraInterface() {
        findAvailableCameras();

        mSelectedCamera = CAMERA_BACK_POSITION;
    }

    /**
     * 寻找合适的摄像头
     */
    private void findAvailableCameras() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int count = Camera.getNumberOfCameras();

        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, info);

            switch (info.facing) {

                case Camera.CameraInfo.CAMERA_FACING_FRONT: {
                    CAMERA_FRONT_POSITION = info.facing;
                }
                break;

                case Camera.CameraInfo.CAMERA_FACING_BACK: {
                    CAMERA_BACK_POSITION = info.facing;
                }
                break;

                default:
                    break;

            }
        }
    }

    /**
     * 设置视频存放目录
     */
    public void setVideoDirectory(String path) {
        mVideoDirectory = path;

        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public void setCameraAngle(Context context) {
        cameraAngle = CameraParamUtil.getCameraDisplayOrientation(context, mSelectedCamera);
    }

    public void setZoom(float zoom, int type) {
        if (mCamera == null) {
            return;
        }

        Camera.Parameters params = mCamera.getParameters();

        if (!params.isZoomSupported() || !params.isSmoothZoomSupported()) {
            return;
        }

        switch (type) {

            case TYPE_RECORDER: {//如果不是录制视频中，上滑不会缩放
                if (!mIsRecorder) {
                    return;
                }

                //每移动50个像素缩放一个级别
                if (zoom >= 0) {
                    int scaleRate = (int) (zoom / 40);
                    if (scaleRate <= params.getMaxZoom() && scaleRate >= nowScaleRate && recordScleRate != scaleRate) {
                        params.setZoom(scaleRate);
                        mCamera.setParameters(params);
                        recordScleRate = scaleRate;
                    }
                }
            }
            break;

            case TYPE_CAPTURE: {
                if (mIsRecorder) {
                    return;
                }

                //每移动50个像素缩放一个级别
                int scaleRate = (int) (zoom / 50);
                if (scaleRate < params.getMaxZoom()) {
                    nowScaleRate += scaleRate;
                    if (nowScaleRate < 0) {
                        nowScaleRate = 0;

                    } else if (nowScaleRate > params.getMaxZoom()) {
                        nowScaleRate = params.getMaxZoom();
                    }

                    params.setZoom(nowScaleRate);
                    mCamera.setParameters(params);
                }
            }
            break;

            default:
                break;

        }

    }

    void setMediaQuality(int quality) {
        this.mediaQuality = quality;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        firstFrame_data = data;
    }

    public void setFlashMode(String flashMode) {
        if (mCamera == null) {
            return;
        }

        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(flashMode);
        mCamera.setParameters(params);
    }

    public interface CameraOpenOverCallback {
        void cameraHasOpened();
    }

    /**
     * open Camera
     */
    void doOpenCamera(CameraOpenOverCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (!CheckPermission.isCameraUseable(mSelectedCamera) && mOnErrorListener != null) {
                mOnErrorListener.onError();
                return;
            }
        }

        if (mCamera == null) {
            openCamera(mSelectedCamera);
        }

        callback.cameraHasOpened();
    }

    private void setFlashModel() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH); //设置camera参数为Torch模式
        mCamera.setParameters(params);
    }

    private synchronized void openCamera(int id) {
        try {
            mCamera = Camera.open(id);

        } catch (Exception e) {
            e.printStackTrace();

            if (mOnErrorListener != null) {
                mOnErrorListener.onError();
            }
        }

        if (Build.VERSION.SDK_INT > 17 && mCamera != null) {
            try {
                mCamera.enableShutterSound(false);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void switchCamera(SurfaceHolder holder, float screenProp) {
        if (mSelectedCamera == CAMERA_BACK_POSITION) {
            mSelectedCamera = CAMERA_FRONT_POSITION;

        } else {
            mSelectedCamera = CAMERA_BACK_POSITION;
        }

        doDestroyCamera();
        openCamera(mSelectedCamera);

        if (Build.VERSION.SDK_INT > 17 && mCamera != null) {
            try {
                this.mCamera.enableShutterSound(false);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        doStartPreview(holder, screenProp);
    }

    /**
     * doStartPreview
     */
    public void doStartPreview(SurfaceHolder holder, float screenProp) {
        if (mIsPreviewing) {
            return;
        }

        if (holder == null) {
            return;
        }

        if (mCamera != null) {
            try {
                Camera.Parameters params = mCamera.getParameters();

                Camera.Size previewSize = CameraParamUtil.getPreviewSize(params.getSupportedPreviewSizes(), 1080, screenProp);
                Camera.Size pictureSize = CameraParamUtil.getPreviewSize(params.getSupportedPictureSizes(), 1080, screenProp);

                params.setPreviewSize(previewSize.width, previewSize.height);

                preview_width = previewSize.width;
                preview_height = previewSize.height;

                params.setPictureSize(pictureSize.width, pictureSize.height);

                if (CameraParamUtil.isSupportedFocusMode(params.getSupportedFocusModes(), Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }

                if (CameraParamUtil.isSupportedPictureFormats(params.getSupportedPictureFormats(), ImageFormat.JPEG)) {
                    params.setPictureFormat(ImageFormat.JPEG);
                    params.setJpegQuality(100);
                }

                mCamera.setParameters(params);
                mCamera.setPreviewDisplay(holder);  //SurfaceView
                mCamera.setDisplayOrientation(cameraAngle);//浏览角度
                mCamera.setPreviewCallback(this); //每一帧回调
                mCamera.startPreview();//启动浏览
                mIsPreviewing = true;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 停止预览
     */
    public void doStopPreview() {
        if (null != mCamera) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                //这句要在stopPreview后执行，不然会卡顿或者花屏
                mCamera.setPreviewDisplay(null);
                mIsPreviewing = false;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 销毁Camera
     */
    void doDestroyCamera() {
        mOnErrorListener = null;

        if (null != mCamera) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                //这句要在stopPreview后执行，不然会卡顿或者花屏
                mCamera.setPreviewDisplay(null);
                mIsPreviewing = false;
                mCamera.release();
                mCamera = null;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 拍照
     */
    public void takePicture(final OnTakePictureListener listener) {
        if (mCamera == null) {
            return;
        }

        int nowAngle = 0;

        if (cameraAngle == 90) {
            nowAngle = Math.abs(angle + cameraAngle) % 360;

        } else if (cameraAngle == 270) {
            nowAngle = Math.abs(cameraAngle - angle);
        }

        final int finalNowAngle = nowAngle;
        mCamera.takePicture(null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                Matrix matrix = new Matrix();
                if (mSelectedCamera == CAMERA_BACK_POSITION) {
                    matrix.setRotate(finalNowAngle);

                } else if (mSelectedCamera == CAMERA_FRONT_POSITION) {
                    matrix.setRotate(360 - finalNowAngle);
                    matrix.postScale(-1, 1);
                }

                bitmap = createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                if (listener != null) {
                    boolean isVertical = (finalNowAngle == 90 || finalNowAngle == 270);
                    byte[] bytes = ImageUtil.bitmap2Bytes(bitmap, Bitmap.CompressFormat.JPEG, 100);

                    listener.onResult(bytes, isVertical);
                }

                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }

                bitmap = null;
            }
        });
    }

    /**
     * 开始录像
     */
    public void startRecord(Surface surface, float screenProp) {
        mCamera.setPreviewCallback(null);
        final int nowAngle = (angle + 90) % 360;

        //获取第一帧图片
        Camera.Parameters parameters = mCamera.getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;

        YuvImage yuv = new YuvImage(firstFrame_data, parameters.getPreviewFormat(), width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] bytes = out.toByteArray();

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Matrix matrix = new Matrix();
        if (mSelectedCamera == CAMERA_BACK_POSITION) {
            matrix.setRotate(nowAngle);

        } else if (mSelectedCamera == CAMERA_FRONT_POSITION) {
            matrix.setRotate(270);
        }
        bitmap = createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        mVideoCoverBytes = ImageUtil.bitmap2Bytes(bitmap, Bitmap.CompressFormat.JPEG, 100);
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        bitmap = null;

        if (mIsRecorder) {
            return;
        }

        if (mCamera == null) {
            openCamera(mSelectedCamera);
        }

        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }

        Camera.Parameters params = mCamera.getParameters();

        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        mCamera.setParameters(params);
        mCamera.unlock();
        mMediaRecorder.reset();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        Camera.Size videoSize;
        if (params.getSupportedVideoSizes() == null) {
            videoSize = CameraParamUtil.getPreviewSize(params.getSupportedPreviewSizes(), 1080, screenProp);

        } else {
            videoSize = CameraParamUtil.getPreviewSize(params.getSupportedVideoSizes(), 1080, screenProp);
        }

        if (videoSize.width == videoSize.height) {
            mMediaRecorder.setVideoSize(preview_width, preview_height);

        } else {
            mMediaRecorder.setVideoSize(videoSize.width, videoSize.height);
        }

        if (mSelectedCamera == CAMERA_FRONT_POSITION) {
            //手机预览倒立的处理
            if (cameraAngle == 270) {
                //横屏
                if (nowAngle == 0) {
                    mMediaRecorder.setOrientationHint(180);

                } else if (nowAngle == 270) {
                    mMediaRecorder.setOrientationHint(270);

                } else {
                    mMediaRecorder.setOrientationHint(90);
                }

            } else {
                if (nowAngle == 90) {
                    mMediaRecorder.setOrientationHint(270);

                } else if (nowAngle == 270) {
                    mMediaRecorder.setOrientationHint(90);

                } else {
                    mMediaRecorder.setOrientationHint(nowAngle);
                }
            }
        } else {
            mMediaRecorder.setOrientationHint(nowAngle);
        }

        if (DeviceUtil.isHuaWeiRongyao()) {
            mMediaRecorder.setVideoEncodingBitRate(4 * 100000);

        } else {
            mMediaRecorder.setVideoEncodingBitRate(mediaQuality);
        }
        mMediaRecorder.setPreviewDisplay(surface);

        mVideoFileName = "video_" + System.currentTimeMillis() + ".mp4";
        mMediaRecorder.setOutputFile(mVideoDirectory + File.separator + mVideoFileName);

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            mIsRecorder = true;

        } catch (IllegalStateException e) {
            e.printStackTrace();

            if (mOnErrorListener != null) {
                mOnErrorListener.onError();
            }

        } catch (IOException e) {
            e.printStackTrace();

            if (mOnErrorListener != null) {
                mOnErrorListener.onError();
            }

        } catch (RuntimeException e) {
        }
    }

    /**
     * 停止录像
     */
    public void stopRecord(boolean isShort, OnRecordListener listener) {
        if (!mIsRecorder) {
            return;
        }

        if (mMediaRecorder == null) {
            return;
        }

        mMediaRecorder.setOnErrorListener(null);
        mMediaRecorder.setOnInfoListener(null);
        mMediaRecorder.setPreviewDisplay(null);

        try {
            mMediaRecorder.stop();

        } catch (IllegalStateException e) {
            e.printStackTrace();
            mMediaRecorder = null;

        } finally {
            if (mMediaRecorder != null) {
                mMediaRecorder.release();
            }

            mMediaRecorder = null;
            mIsRecorder = false;
        }

        String filePath = mVideoDirectory + File.separator + mVideoFileName;

        if (isShort) {
            FileUtil.deleteFile(filePath);

            if (listener != null) {
                listener.onShort();
            }

        } else {
            doStopPreview();

            if (listener != null) {
                listener.onResult(filePath, mVideoCoverBytes);
            }
        }
    }

    public void handleFocus(int screenWidth, int screenHeight, float x, float y, final OnFocusListener listener) {
        if (mCamera == null) {
            if (listener != null) {
                listener.onFailure();
            }
            return;
        }

        final Camera.Parameters params = mCamera.getParameters();
        Rect focusRect = calculateTapArea(screenWidth, screenHeight, x, y, 1f);
        mCamera.cancelAutoFocus();

        if (params.getMaxNumFocusAreas() <= 0) {
            if (listener != null) {
                listener.onFailure();
            }
            return;
        }

        List<Camera.Area> focusAreas = new ArrayList<>();
        focusAreas.add(new Camera.Area(focusRect, 800));
        params.setFocusAreas(focusAreas);

        final String currentFocusMode = params.getFocusMode();

        try {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
            mCamera.autoFocus(new Camera.AutoFocusCallback() {

                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        Camera.Parameters params = camera.getParameters();
                        params.setFocusMode(currentFocusMode);
                        camera.setParameters(params);

                        if (listener != null) {
                            listener.onSuccess();
                        }

                    } else {
                        if (listener != null) {
                            listener.onFailure();
                        }
                    }
                }

            });

        } catch (Exception e) {
            e.printStackTrace();

            if (listener != null) {
                listener.onFailure();
            }
        }
    }

    private static Rect calculateTapArea(int screenWidth, int screenHeight, float x, float y, float coefficient) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / screenWidth * 2000 - 1000);
        int centerY = (int) (y / screenHeight * 2000 - 1000);
        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;

        } else if (x < min) {
            return min;
        }

        return x;
    }

    void registerSensorManager(Context context) {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }

        mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    void unregisterSensorManager(Context context) {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }

        mSensorManager.unregisterListener(mSensorEventListener);
    }

    void isPreview(boolean res) {
        mIsPreviewing = res;
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent event) {
            if (Sensor.TYPE_ACCELEROMETER != event.sensor.getType()) {
                return;
            }

            float[] values = event.values;
            angle = AngleUtil.getSensorAngle(values[0], values[1]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

    };

    public static void release() {
        if (mCameraInterface != null) {
            mCameraInterface = null;
        }
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public interface OnErrorListener {

        void onError();

    }

    public interface OnTakePictureListener {

        void onResult(byte[] bytes, boolean isVertical);

    }

    public interface OnFocusListener {

        void onSuccess();

        void onFailure();

    }

    public interface OnRecordListener {

        void onShort();

        void onResult(String filePath, byte[] coverBytes);

    }

}