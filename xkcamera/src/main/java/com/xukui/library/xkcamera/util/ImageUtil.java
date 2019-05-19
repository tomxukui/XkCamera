package com.xukui.library.xkcamera.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayOutputStream;

public class ImageUtil {

    /**
     * bitmap转byteArr
     *
     * @param bitmap  bitmap对象
     * @param format  格式
     * @param quality 质量 0-100
     */
    public static byte[] bitmap2Bytes(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        if (bitmap == null) {
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(format, quality, baos);
        return baos.toByteArray();
    }

    /**
     * byteArr转bitmap
     *
     * @param bytes 字节数组
     */
    public static Bitmap bytes2Bitmap(byte[] bytes) {
        return (bytes == null || bytes.length == 0) ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * byteArr转drawable
     *
     * @param res   resources对象
     * @param bytes 字节数组
     */
    public static Drawable bytes2Drawable(Resources res, byte[] bytes) {
        return res == null ? null : new BitmapDrawable(res, bytes2Bitmap(bytes));
    }

}