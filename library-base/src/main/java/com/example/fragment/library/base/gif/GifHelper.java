package com.example.fragment.library.base.gif;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.example.fragment.library.base.gif.encoder.AnimatedGifEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import pl.droidsonroids.gif.GifDrawable;

/**
 * 在 android-gif-drawable 和 android-gif-encoder 基础上实现GIF格式图片压缩
 * https://github.com/koral--/android-gif-drawable
 * https://github.com/nbadal/android-gif-encoder
 */
public class GifHelper {

    public static void generateGIF(ArrayList<Bitmap> bitmaps, String savePath) {
        new Thread(() -> {
            ByteArrayOutputStream bos = null;
            FileOutputStream outStream = null;
            try {
                bos = new ByteArrayOutputStream();
                AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                encoder.start(bos);
                for (Bitmap bitmap : bitmaps) {
                    encoder.addFrame(bitmap);
                }
                encoder.finish();
                outStream = new FileOutputStream(savePath);
                outStream.write(bos.toByteArray());
                outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bos != null) {
                        bos.close();
                    }
                    if (outStream != null) {
                        outStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    /**
     * 先把GIF分解成bitmap，进行缩放操作，最后合成一张新的GIF
     *
     * @param gifFile      gif文件
     * @param targetLength 压缩的目标Length
     * @param savePath     压缩后的保存路径
     */
    public static void compressScale(File gifFile, long targetLength, String savePath) {
        new Thread(() -> {
            GifDrawable gifDrawable = null;
            ByteArrayOutputStream bos = null;
            FileOutputStream outStream = null;
            try {
                long gifFileLength = gifFile.length();
                if (gifFileLength > targetLength) {
                    gifDrawable = new GifDrawable(gifFile);
                    int gifFrames = gifDrawable.getNumberOfFrames();
                    int gifDuration = gifDrawable.getDuration();
                    bos = new ByteArrayOutputStream();
                    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                    encoder.setRepeat(0);
                    encoder.start(bos);
                    encoder.setDelay(gifDuration / gifFrames);
                    //把 targetLength 除以 gifFileLength 的商开平方就得到缩放值
                    float scale = (float) Math.sqrt(targetLength / (double) gifFileLength);
                    //将每一帧图片缩放，最后合成新的 gif
                    //按照此算法合成的 gif 总比预期大，希望有大佬指出原因
                    for (int i = 0; i < gifFrames; i++) {
                        Bitmap src = gifDrawable.seekToFrameAndGet(i);
                        Matrix m = new Matrix();
                        m.setScale(scale, scale);
                        int width = src.getWidth();
                        int height = src.getHeight();
                        src = Bitmap.createBitmap(src, 0, 0, width, height, m, true);
                        encoder.addFrame(src);
                    }
                    encoder.finish();
                    outStream = new FileOutputStream(savePath);
                    outStream.write(bos.toByteArray());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (gifDrawable != null) {
                        gifDrawable.recycle();
                    }
                    if (bos != null) {
                        bos.close();
                    }
                    if (outStream != null) {
                        outStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 先把GIF分解成bitmap，进行减帧操作，最后合成一张新的GIF
     *
     * @param gifFile   gif文件
     * @param maxFrames 最大帧数
     * @param savePath  压缩后的保存路径
     */
    public static void compressFrame(File gifFile, int maxFrames, String savePath) {
        new Thread(() -> {
            GifDrawable gifDrawable = null;
            ByteArrayOutputStream bos = null;
            FileOutputStream outStream = null;
            try {
                gifDrawable = new GifDrawable(gifFile);
                int gifFrames = gifDrawable.getNumberOfFrames();
                int gifDuration = gifDrawable.getDuration();
                if (gifFrames > maxFrames) {
                    bos = new ByteArrayOutputStream();
                    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                    encoder.setRepeat(0);
                    encoder.start(bos);
                    encoder.setDelay(gifDuration / gifFrames);
                    if (maxFrames > gifFrames / 2) {
                        long surplusFrames = gifFrames - maxFrames;
                        int mold = (int) Math.ceil(gifFrames / (double) surplusFrames);
                        for (int i = 0; i < gifFrames; i++) {
                            if (i % mold != 1) {
                                encoder.addFrame(gifDrawable.seekToFrameAndGet(i));
                            }
                        }
                    } else {
                        int mold = (int) Math.ceil(gifFrames / (double) maxFrames);
                        for (int i = 0; i < gifFrames; i++) {
                            if (i % mold == 0) {
                                encoder.addFrame(gifDrawable.seekToFrameAndGet(i));
                            }
                        }
                    }
                    encoder.finish();
                    outStream = new FileOutputStream(savePath);
                    outStream.write(bos.toByteArray());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (gifDrawable != null) {
                        gifDrawable.recycle();
                    }
                    if (bos != null) {
                        bos.close();
                    }
                    if (outStream != null) {
                        outStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
