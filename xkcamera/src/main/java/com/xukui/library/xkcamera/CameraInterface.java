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
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.xukui.library.xkcamera.util.AngleUtil;
import com.xukui.library.xkcamera.util.CameraParamUtil;
import com.xukui.library.xkcamera.util.CheckPermission;
import com.xukui.library.xkcamera.util.DeviceUtil;
import com.xukui.library.xkcamera.util.FileUtil;
import com.xukui.library.xkcamera.util.ScreenUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.Bitmap.createBitmap;

public class CameraInterface implements Camera.PreviewCallback {

    private volatile static CameraInterface mCameraInterface;

    private Camera mCamera;
    private Camera.Parameters mParams;
    private boolean isPreviewing = false;

    private int SELECTED_CAMERA = -1;
    private int CAMERA_POST_POSITION = -1;
    private int CAMERA_FRONT_POSITION = -1;

    private float screenProp = -1.0f;

    private boolean isRecorder = false;
    private MediaRecorder mediaRecorder;
    private String videoFileName;
    private String saveVideoPath;
    private String videoFileAbsPath;
    private Bitmap videoFirstFrame;

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
    private SensorManager sm = null;

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

    public void setCameraAngle(Context context) {
        cameraAngle = CameraParamUtil.getCameraDisplayOrientation(context, SELECTED_CAMERA);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void setSaveVideoPath(String saveVideoPath) {
        this.saveVideoPath = saveVideoPath;

        File file = new File(saveVideoPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public void setZoom(float zoom, int type) {
        if (mCamera == null) {
            return;
        }

        if (mParams == null) {
            mParams = mCamera.getParameters();
        }

        if (!mParams.isZoomSupported() || !mParams.isSmoothZoomSupported()) {
            return;
        }

        switch (type) {

            case TYPE_RECORDER: {//如果不是录制视频中，上滑不会缩放
                if (!isRecorder) {
                    return;
                }

                //每移动50个像素缩放一个级别
                if (zoom >= 0) {
                    int scaleRate = (int) (zoom / 40);
                    if (scaleRate <= mParams.getMaxZoom() && scaleRate >= nowScaleRate && recordScleRate != scaleRate) {
                        mParams.setZoom(scaleRate);
                        mCamera.setParameters(mParams);
                        recordScleRate = scaleRate;
                    }
                }
            }
            break;

            case TYPE_CAPTURE: {
                if (isRecorder) {
                    return;
                }

                //每移动50个像素缩放一个级别
                int scaleRate = (int) (zoom / 50);
                if (scaleRate < mParams.getMaxZoom()) {
                    nowScaleRate += scaleRate;
                    if (nowScaleRate < 0) {
                        nowScaleRate = 0;

                    } else if (nowScaleRate > mParams.getMaxZoom()) {
                        nowScaleRate = mParams.getMaxZoom();
                    }

                    mParams.setZoom(nowScaleRate);
                    mCamera.setParameters(mParams);
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

    private CameraInterface() {
        findAvailableCameras();
        SELECTED_CAMERA = CAMERA_POST_POSITION;
        saveVideoPath = "";
    }

    /**
     * open Camera
     */
    void doOpenCamera(CameraOpenOverCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (!CheckPermission.isCameraUseable(SELECTED_CAMERA) && mOnErrorListener != null) {
                mOnErrorListener.onError();
                return;
            }
        }

        if (mCamera == null) {
            openCamera(SELECTED_CAMERA);
        }

        callback.cameraHasOpened();
    }

    private void setFlashModel() {
        mParams = mCamera.getParameters();
        mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH); //设置camera参数为Torch模式
        mCamera.setParameters(mParams);
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
        if (SELECTED_CAMERA == CAMERA_POST_POSITION) {
            SELECTED_CAMERA = CAMERA_FRONT_POSITION;

        } else {
            SELECTED_CAMERA = CAMERA_POST_POSITION;
        }

        doDestroyCamera();
        openCamera(SELECTED_CAMERA);

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
        if (isPreviewing) {
            return;
        }

        if (this.screenProp < 0) {
            this.screenProp = screenProp;
        }

        if (holder == null) {
            return;
        }

        if (mCamera != null) {
            try {
                mParams = mCamera.getParameters();

                Camera.Size previewSize = CameraParamUtil.getPreviewSize(mParams.getSupportedPreviewSizes(), 1080, screenProp);
                Camera.Size pictureSize = CameraParamUtil.getPreviewSize(mParams.getSupportedPictureSizes(), 1080, screenProp);

                mParams.setPreviewSize(previewSize.width, previewSize.height);

                preview_width = previewSize.width;
                preview_height = previewSize.height;

                mParams.setPictureSize(pictureSize.width, pictureSize.height);

                if (CameraParamUtil.isSupportedFocusMode(mParams.getSupportedFocusModes(), Camera.Parameters.FOCUS_MODE_AUTO)) {
                    mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }

                if (CameraParamUtil.isSupportedPictureFormats(mParams.getSupportedPictureFormats(), ImageFormat.JPEG)) {
                    mParams.setPictureFormat(ImageFormat.JPEG);
                    mParams.setJpegQuality(100);
                }

                mCamera.setParameters(mParams);
                mParams = mCamera.getParameters();
                mCamera.setPreviewDisplay(holder);  //SurfaceView
                mCamera.setDisplayOrientation(cameraAngle);//浏览角度
                mCamera.setPreviewCallback(this); //每一帧回调
                mCamera.startPreview();//启动浏览
                isPreviewing = true;

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
                isPreviewing = false;

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
                isPreviewing = false;
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
    private int nowAngle;

    public void takePicture(final OnTakePictureListener listener) {
        if (mCamera == null) {
            return;
        }

        switch (cameraAngle) {

            case 90: {
                nowAngle = Math.abs(angle + cameraAngle) % 360;
            }
            break;

            case 270: {
                nowAngle = Math.abs(cameraAngle - angle);
            }
            break;

            default:
                break;

        }

        mCamera.takePicture(null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Matrix matrix = new Matrix();

                if (SELECTED_CAMERA == CAMERA_POST_POSITION) {
                    matrix.setRotate(nowAngle);

                } else if (SELECTED_CAMERA == CAMERA_FRONT_POSITION) {
                    matrix.setRotate(360 - nowAngle);
                    matrix.postScale(-1, 1);
                }

                bitmap = createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                if (listener != null) {
                    boolean isVertical = (nowAngle == 90 || nowAngle == 270);
                    listener.onResult(bitmap, isVertical);
                }
            }
        });
    }

    //启动录像
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
        videoFirstFrame = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Matrix matrix = new Matrix();
        if (SELECTED_CAMERA == CAMERA_POST_POSITION) {
            matrix.setRotate(nowAngle);
        } else if (SELECTED_CAMERA == CAMERA_FRONT_POSITION) {
            matrix.setRotate(270);
        }
        videoFirstFrame = createBitmap(videoFirstFrame, 0, 0, videoFirstFrame.getWidth(), videoFirstFrame.getHeight(), matrix, true);

        if (isRecorder) {
            return;
        }

        if (mCamera == null) {
            openCamera(SELECTED_CAMERA);
        }

        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }

        if (mParams == null) {
            mParams = mCamera.getParameters();
        }

        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        mCamera.setParameters(mParams);
        mCamera.unlock();
        mediaRecorder.reset();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        Camera.Size videoSize;
        if (mParams.getSupportedVideoSizes() == null) {
            videoSize = CameraParamUtil.getPreviewSize(mParams.getSupportedPreviewSizes(), 1080, screenProp);

        } else {
            videoSize = CameraParamUtil.getPreviewSize(mParams.getSupportedVideoSizes(), 1080, screenProp);
        }

        if (videoSize.width == videoSize.height) {
            mediaRecorder.setVideoSize(preview_width, preview_height);

        } else {
            mediaRecorder.setVideoSize(videoSize.width, videoSize.height);
        }

        if (SELECTED_CAMERA == CAMERA_FRONT_POSITION) {
            //手机预览倒立的处理
            if (cameraAngle == 270) {
                //横屏
                if (nowAngle == 0) {
                    mediaRecorder.setOrientationHint(180);

                } else if (nowAngle == 270) {
                    mediaRecorder.setOrientationHint(270);

                } else {
                    mediaRecorder.setOrientationHint(90);
                }

            } else {
                if (nowAngle == 90) {
                    mediaRecorder.setOrientationHint(270);

                } else if (nowAngle == 270) {
                    mediaRecorder.setOrientationHint(90);

                } else {
                    mediaRecorder.setOrientationHint(nowAngle);
                }
            }
        } else {
            mediaRecorder.setOrientationHint(nowAngle);
        }

        if (DeviceUtil.isHuaWeiRongyao()) {
            mediaRecorder.setVideoEncodingBitRate(4 * 100000);

        } else {
            mediaRecorder.setVideoEncodingBitRate(mediaQuality);
        }
        mediaRecorder.setPreviewDisplay(surface);

        videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
        if (saveVideoPath.equals("")) {
            saveVideoPath = Environment.getExternalStorageDirectory().getPath();
        }
        videoFileAbsPath = saveVideoPath + File.separator + videoFileName;
        mediaRecorder.setOutputFile(videoFileAbsPath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecorder = true;

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
        if (!isRecorder) {
            return;
        }

        if (mediaRecorder == null) {
            return;
        }

        mediaRecorder.setOnErrorListener(null);
        mediaRecorder.setOnInfoListener(null);
        mediaRecorder.setPreviewDisplay(null);

        try {
            mediaRecorder.stop();

        } catch (IllegalStateException e) {
            e.printStackTrace();
            mediaRecorder = null;

        } finally {
            if (mediaRecorder != null) {
                mediaRecorder.release();
            }

            mediaRecorder = null;
            isRecorder = false;
        }

        if (isShort) {
            FileUtil.deleteFile(videoFileAbsPath);

            if (listener != null) {
                listener.onShort();
            }

        } else {
            doStopPreview();

            if (listener != null) {
                String fileName = saveVideoPath + File.separator + videoFileName;
                listener.onResult(fileName, videoFirstFrame);
            }
        }
    }

    private void findAvailableCameras() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraNum = Camera.getNumberOfCameras();

        for (int i = 0; i < cameraNum; i++) {
            Camera.getCameraInfo(i, info);

            switch (info.facing) {

                case Camera.CameraInfo.CAMERA_FACING_FRONT: {
                    CAMERA_FRONT_POSITION = info.facing;
                }
                break;

                case Camera.CameraInfo.CAMERA_FACING_BACK: {
                    CAMERA_POST_POSITION = info.facing;
                }
                break;

                default:
                    break;

            }
        }
    }

    public void handleFocus(final Context context, final float x, final float y, final OnFocusListener listener) {
        if (mCamera == null) {
            if (listener != null) {
                listener.onFailure();
            }
            return;
        }

        final Camera.Parameters params = mCamera.getParameters();
        Rect focusRect = calculateTapArea(x, y, 1f, context);
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

    private static Rect calculateTapArea(float x, float y, float coefficient, Context context) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / ScreenUtils.getScreenWidth(context) * 2000 - 1000);
        int centerY = (int) (y / ScreenUtils.getScreenHeight(context) * 2000 - 1000);
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
        if (sm == null) {
            sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }

        sm.registerListener(mSensorEventListener, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    void unregisterSensorManager(Context context) {
        if (sm == null) {
            sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }

        sm.unregisterListener(mSensorEventListener);
    }

    void isPreview(boolean res) {
        this.isPreviewing = res;
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

        void onResult(Bitmap bitmap, boolean isVertical);

    }

    public interface OnFocusListener {

        void onSuccess();

        void onFailure();

    }

    public interface OnRecordListener {

        void onShort();

        void onResult(String url, Bitmap firstFrame);

    }

}