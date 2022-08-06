package com.example.fragment.library.base.gif;

import android.graphics.Bitmap;

import com.example.fragment.library.base.gif.encoder.AnimatedGifEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import pl.droidsonroids.gif.GifDrawable;

/**
 * 感谢 android-gif-drawable 和 android-gif-encoder ，在此基础上实现GIF格式图片压缩
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
     * 先把GIF分解成bitmap，进行减帧操作，最后合成一张新的GIF
     *
     * @param gifFile      gif文件
     * @param targetLength 压缩的目标体积
     * @param savePath     压缩后的保存路径
     */
    public static void compressFrame(File gifFile, long targetLength, String savePath) {
        new Thread(() -> {
            GifDrawable gifDrawable = null;
            ByteArrayOutputStream bos = null;
            FileOutputStream outStream = null;
            try {
                long gifFileLength = gifFile.length();
                gifDrawable = new GifDrawable(gifFile);
                int gifFrames = gifDrawable.getNumberOfFrames();
                int gifDuration = gifDrawable.getDuration();
                if (gifFileLength > targetLength) {
                    bos = new ByteArrayOutputStream();
                    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                    encoder.setRepeat(0);
                    encoder.start(bos);
                    encoder.setDelay(gifDuration / gifFrames);
                    if (targetLength > gifFileLength / 2) {
                        long surplusLength = gifFileLength - targetLength;
                        int mold = (int) Math.ceil(gifFileLength * 1.0 / surplusLength);
                        for (int i = 0; i < gifFrames; i++) {
                            if (i % mold != 1) {
                                encoder.addFrame(gifDrawable.seekToFrameAndGet(i));
                            }
                        }
                    } else {
                        int mold = (int) Math.ceil(gifFileLength * 1.0 / targetLength);
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
                        int mold = (int) Math.ceil(gifFrames * 1.0 / surplusFrames);
                        for (int i = 0; i < gifFrames; i++) {
                            if (i % mold != 1) {
                                encoder.addFrame(gifDrawable.seekToFrameAndGet(i));
                            }
                        }
                    } else {
                        int mold = (int) Math.ceil(gifFrames * 1.0 / maxFrames);
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
