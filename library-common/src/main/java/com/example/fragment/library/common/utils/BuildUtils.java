package cn.colg.base.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;

import cn.colg.base.utilcode.util.SPUtils;

@SuppressLint("HardwareIds")
public class DevUtils {

    private static DevUtils singleton;

    private DevUtils() {
    }

    public static DevUtils get() {
        if (singleton == null) {
            synchronized (ForumUtils.class) {
                if (singleton == null) {
                    singleton = new DevUtils();
                }
            }
        }
        return singleton;
    }

    private String brand;
    private String model;
    private String serial;
    private String manufacturer;
    private String release;

    public String getBrand() {
        if (TextUtils.isEmpty(brand)) {
            brand = SPUtils.getInstance().getString("BRAND");
            if (TextUtils.isEmpty(brand)) {
                brand = Build.BRAND;
                SPUtils.getInstance().put("BRAND", brand);
            }
        }
        return brand;
    }

    public String getModel() {
        if (TextUtils.isEmpty(model)) {
            model = SPUtils.getInstance().getString("MODEL");
            if (TextUtils.isEmpty(model)) {
                model = Build.MODEL;
                SPUtils.getInstance().put("MODEL", model);
            }
        }
        return model;
    }

    public String getSerial() {
        if (TextUtils.isEmpty(serial)) {
            serial = SPUtils.getInstance().getString("SERIAL");
            if (TextUtils.isEmpty(serial)) {
                serial = Build.SERIAL;
                SPUtils.getInstance().put("SERIAL", serial);
            }
        }
        return serial;
    }

    public String getManufacturer() {
        if (TextUtils.isEmpty(manufacturer)) {
            manufacturer = SPUtils.getInstance().getString("MANUFACTURER");
            if (TextUtils.isEmpty(manufacturer)) {
                manufacturer = Build.MANUFACTURER;
                SPUtils.getInstance().put("MANUFACTURER", manufacturer);
            }
        }
        return manufacturer;
    }

    public String getRelease() {
        if (TextUtils.isEmpty(release)) {
            release = SPUtils.getInstance().getString("MANUFACTURER");
            if (TextUtils.isEmpty(release)) {
                release = Build.VERSION.RELEASE;
                SPUtils.getInstance().put("MANUFACTURER", release);
            }
        }
        return release;
    }

}
