package com.xukui.library.xkcamera.util;

import android.text.TextUtils;

import java.io.File;

public class FileUtil {

    /**
     * 删除文件
     */
    public static void deleteFile(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);

            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

}