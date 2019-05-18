package com.xukui.library.xkcamera.util;

import android.content.Context;
import android.hardware.Camera;
import android.view.Surface;
import android.view.WindowManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraParamUtil {

    /**
     * 获取预览的尺寸
     */
    public static Camera.Size getPreviewSize(List<Camera.Size> list, int th, float rate) {
        Collections.sort(list, new Comparator<Camera.Size>() {

            @Override
            public int compare(Camera.Size s1, Camera.Size s2) {
                int len = s1.width - s2.width;

                if (len == 0) {
                    return s1.height - s2.height;

                } else {
                    return len;
                }
            }

        });

        for (int i = 0; i < list.size(); i++) {
            Camera.Size size = list.get(i);

            if ((size.width > th) && equalRate(size, rate)) {
                return list.get(i);
            }
        }

        return getBestSize(list, rate);
    }

    /**
     * 如果没有合适的尺寸，获取最佳尺寸
     */
    private static Camera.Size getBestSize(List<Camera.Size> list, float rate) {
        float previewDisparity = 100;
        int index = 0;

        for (int i = 0; i < list.size(); i++) {
            Camera.Size cur = list.get(i);

            float prop = (float) cur.width / (float) cur.height;

            if (Math.abs(rate - prop) < previewDisparity) {
                previewDisparity = Math.abs(rate - prop);
                index = i;
            }
        }

        return list.get(index);
    }

    private static boolean equalRate(Camera.Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);

        return Math.abs(r - rate) <= 0.1;
    }

    public static boolean isSupportedFocusMode(List<String> focusList, String focusMode) {
        for (int i = 0; i < focusList.size(); i++) {
            if (focusMode.equals(focusList.get(i))) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSupportedPictureFormats(List<Integer> supportedPictureFormats, int jpeg) {
        for (int i = 0; i < supportedPictureFormats.size(); i++) {
            if (jpeg == supportedPictureFormats.get(i)) {
                return true;
            }
        }

        return false;
    }

    private static class CameraSizeComparator implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;

            } else if (lhs.width > rhs.width) {
                return 1;

            } else {
                return -1;
            }
        }

    }

    public static int getCameraDisplayOrientation(Context context, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {

            case Surface.ROTATION_0: {
                degrees = 0;
            }
            break;

            case Surface.ROTATION_90: {
                degrees = 90;
            }
            break;

            case Surface.ROTATION_180: {
                degrees = 180;
            }
            break;

            case Surface.ROTATION_270: {
                degrees = 270;
            }
            break;

            default:
                break;

        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;

        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

}