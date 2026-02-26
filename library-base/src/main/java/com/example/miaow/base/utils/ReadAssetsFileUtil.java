package com.example.miaow.base.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 读取Assets目录下的文件工具类
 */
public class ReadAssetsFileUtil {
    /**
     * 读取assets下的txt文件，返回utf-8 String
     *
     * @param fileName 不包括后缀
     */
    public static String readAssetsTxt(Context context, String fileName) {
        try {
            //Return an AssetManager instance for your application's package
            InputStream is = context.getAssets().open(fileName + ".txt");
            int size = is.available();
            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            int i = is.read(buffer);
            is.close();
            // Convert the buffer into a string.
            // Finally stick the string into the text view.
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Should never happen!
            Log.e(ReadAssetsFileUtil.class.getName(), Objects.requireNonNull(e.getMessage()));
        }
        return "";
    }

    //读取方法
    public static String getJson(Context context, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assetManager = context.getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assetManager.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            Log.e(ReadAssetsFileUtil.class.getName(), Objects.requireNonNull(e.getMessage()));
        }
        return stringBuilder.toString();
    }

}
