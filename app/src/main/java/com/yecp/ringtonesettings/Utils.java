package com.yecp.ringtonesettings;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;

public class Utils {
    public static String getMediaDataFromURI(Context context, Uri contentUri, String dataType) {
        String[] projection = { dataType };
        Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(dataType);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        }
        return null;
    }

    public static String removeFilenameSuffix(String filePath) {
        if (filePath == null)
            return "";

        File file = new File(filePath);
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName; // No extension found
        }
        return fileName.substring(0, lastDotIndex);
    }

    public static boolean isExternalResource(Uri uri) {
        // 检查 URI 是否为外部存储
        return uri != null && uri.toString().contains("//media/external");
    }
}
