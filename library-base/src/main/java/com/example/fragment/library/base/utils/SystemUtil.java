package com.example.fragment.library.base.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.fragment.library.base.provider.BaseContent;

import static android.content.Context.ACTIVITY_SERVICE;

public class SystemUtil {

    /**
     * 获取应用程序名称
     */
    public static synchronized String getAppName() {
        return getAppName(BaseContent.get());
    }

    public static synchronized String getAppName(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            int labelRes = info.applicationInfo.labelRes;
            return context.getResources().getString(labelRes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取应用程序版本名称
     */
    public static synchronized String getVersionName() {
        return getVersionName(BaseContent.get());
    }

    public static synchronized String getVersionName(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取应用程序版本号
     */
    public static synchronized int getVersionCode() {
        return getVersionCode(BaseContent.get());
    }

    public static synchronized int getVersionCode(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取应用程序包名
     */
    public static synchronized String getPackageName() {
        return getPackageName(BaseContent.get());
    }

    public static synchronized String getPackageName(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.packageName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 跳转应用详情
     */
    public static void gotoAppDetailsSettings() {
        gotoAppDetailsSettings(BaseContent.get());
    }

    public static void gotoAppDetailsSettings(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
        }
        context.startActivity(intent);
    }

    /**
     * 判断服务是否运行
     *
     * @param serviceName
     * @return
     */
    public static boolean isServiceRunning(String serviceName) {
        return isRunningService(BaseContent.get(), serviceName);
    }

    public static boolean isRunningService(Context context, String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断本应用是否已经位于最前端
     *
     * @return 本应用已经位于最前端时，返回 true；否则返回 false
     */
    public static boolean isRunningForeground() {
        return isRunningForeground(BaseContent.get());
    }

    public static boolean isRunningForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        /**枚举进程*/
        for (ActivityManager.RunningAppProcessInfo info : manager.getRunningAppProcesses()) {
            if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (info.processName.equals(context.getApplicationInfo().processName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将本应用置顶到最前端
     * 当本应用位于后台时，则将它切换到最前端
     */
    public static void setTopApp() {
        setTopApp(BaseContent.get());
    }

    public static void setTopApp(Context context) {
        if (!isRunningForeground()) {
            /**获取ActivityManager*/
            ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            /**获得当前运行的task(任务)*/
            for (ActivityManager.RunningTaskInfo info : manager.getRunningTasks(Integer.MAX_VALUE)) {
                /**找到本应用的 task，并将它切换到前台*/
                if (info.topActivity.getPackageName().equals(context.getPackageName())) {
                    manager.moveTaskToFront(info.id, 0);
                    break;
                }
            }
        }
    }

    /**
     * 解锁屏幕
     */
    @SuppressLint("InvalidWakeLockTag")
    public static void acquireWakeLock() {
        acquireWakeLock(BaseContent.get());
    }

    @SuppressLint("InvalidWakeLockTag")
    public static void acquireWakeLock(Context context) {
        PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        int levelAndFlags = PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
        PowerManager.WakeLock wakeLock = manager.newWakeLock(levelAndFlags, "bright");
        if (!manager.isScreenOn()) {
            wakeLock.acquire();
            wakeLock.release();
        }
    }

    /**
     * app灰白化,特殊节日使用
     */
    public static void appGraying(Activity activity) {
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        activity.getWindow().getDecorView().setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * 设置状态栏颜色
     *
     * @param resId 颜色资源id
     * @param dark  状态栏字体模式
     */
    public static void setStatusBarTheme(Activity activity, int resId, boolean dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, resId));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int lFlags = activity.getWindow().getDecorView().getSystemUiVisibility();
            activity.getWindow().getDecorView().setSystemUiVisibility(dark ? (lFlags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (lFlags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
        }
    }

}
