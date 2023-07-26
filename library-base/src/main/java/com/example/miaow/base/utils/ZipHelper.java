package com.example.miaow.base.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipHelper {

    /**
     * 压缩文件
     *
     * @param file        需要压缩的文件
     * @param zipFilePath 被压缩后存放的路径
     */
    public static File zipFiles(File file, String zipFilePath) {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFilePath)));
            recursionZip(zos, file);
            zos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (zos != null) {
                    zos.closeEntry();
                    zos.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return new File(zipFilePath);
    }

    private static void recursionZip(ZipOutputStream zipOut, File file) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File file1 : files) {
                if (file1 == null) {
                    continue;
                }
                recursionZip(zipOut, file1);
            }
        } else {
            byte[] buf = new byte[1024];
            InputStream input = new BufferedInputStream(new FileInputStream(file));
            zipOut.putNextEntry(new ZipEntry(file.getPath() + File.separator + file.getName()));
            int len;
            while ((len = input.read(buf)) != -1) {
                zipOut.write(buf, 0, len);
            }
            input.close();
        }
    }

    /**
     * 解压文件
     *
     * @param zipPath   压缩文件目录
     * @param unZipPath 解压后的目录
     */
    public static void unZipFile(String zipPath, String unZipPath) {
        BufferedOutputStream bos = null;
        ZipInputStream zis = null;
        try {
            String filename;
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipPath)));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;
            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                createSubFolders(filename, unZipPath);
                if (ze.isDirectory()) {
                    File fmd = new File(unZipPath + filename);
                    //noinspection ResultOfMethodCallIgnored
                    fmd.mkdirs();
                    continue;
                }
                bos = new BufferedOutputStream(new FileOutputStream(unZipPath + filename));
                while ((count = zis.read(buffer)) != -1) {
                    bos.write(buffer, 0, count);
                }
                bos.flush();
                bos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zis != null) {
                    zis.closeEntry();
                    zis.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createSubFolders(String filename, String path) {
        String[] subFolders = filename.split("/");
        if (subFolders.length <= 1) {
            return;
        }
        String pathNow = path;
        for (int i = 0; i < subFolders.length - 1; ++i) {
            pathNow = pathNow + subFolders[i] + "/";
            File fmd = new File(pathNow);
            if (fmd.exists()) {
                continue;
            }
            //noinspection ResultOfMethodCallIgnored
            fmd.mkdirs();
        }
    }

}