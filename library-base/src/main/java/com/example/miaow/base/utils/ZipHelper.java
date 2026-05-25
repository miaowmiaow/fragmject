package com.example.miaow.base.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipHelper {

    private static final String TAG = "ZipHelper";

    /**
     * 压缩文件
     *
     * @param file        需要压缩的文件
     * @param zipFilePath 被压缩后存放的路径
     */
    public static File zipFiles(File file, String zipFilePath) {
        // 使用 try-with-resources 保证 zos 一定被关闭
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipFilePath)))) {
            recursionZip(zos, file);
            zos.flush();
        } catch (Exception e) {
            // 之前用 Objects.requireNonNull(e.getMessage()) 当 message 为 null 时会再抛 NPE，
            // 把"日志记录"代码变成"崩溃源"，这里改为安全打印。
            Log.e(TAG, "zipFiles failed: " + zipFilePath, e);
        }
        return new File(zipFilePath);
    }

    private static void recursionZip(ZipOutputStream zipOut, File file) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File child : files) {
                if (child == null) {
                    continue;
                }
                recursionZip(zipOut, child);
            }
        } else {
            // 之前 input 流仅在末尾 close()，write 抛异常时会泄漏；改为 try-with-resources。
            try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
                zipOut.putNextEntry(new ZipEntry(file.getPath() + File.separator + file.getName()));
                byte[] buf = new byte[1024];
                int len;
                while ((len = input.read(buf)) != -1) {
                    zipOut.write(buf, 0, len);
                }
            }
        }
    }

    /**
     * 解压文件
     *
     * @param zipPath   压缩文件目录
     * @param unZipPath 解压后的目录
     */
    public static void unZipFile(String zipPath, String unZipPath) {
        // 之前 bos 在 while 循环里反复赋值并放在外部 finally 中关闭，
        // 异常时存在错配关闭风险。改为每个 entry 自带 try-with-resources。
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipPath)))) {
            byte[] buffer = new byte[1024];
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String filename = ze.getName();
                createSubFolders(filename, unZipPath);
                if (ze.isDirectory()) {
                    File fmd = new File(unZipPath + filename);
                    //noinspection ResultOfMethodCallIgnored
                    fmd.mkdirs();
                    continue;
                }
                try (OutputStream bos = new BufferedOutputStream(
                        new FileOutputStream(unZipPath + filename))) {
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, count);
                    }
                    bos.flush();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "unZipFile failed: " + zipPath, e);
        }
    }

    private static void createSubFolders(String filename, String path) {
        String[] subFolders = filename.split("/");
        if (subFolders.length <= 1) {
            return;
        }
        StringBuilder pathNow = new StringBuilder(path);
        for (int i = 0; i < subFolders.length - 1; ++i) {
            pathNow.append(subFolders[i]).append("/");
            File fmd = new File(pathNow.toString());
            if (fmd.exists()) {
                continue;
            }
            //noinspection ResultOfMethodCallIgnored
            fmd.mkdirs();
        }
    }

}