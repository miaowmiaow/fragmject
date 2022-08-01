package com.example.fragment.library.common.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;

import com.example.fragment.library.base.db.KVDatabase;

public class BuildUtils {

    private static BuildUtils singleton;

    public static String getBrand() {
        return BuildUtils.get().brand();
    }

    public static String getModel(){
        return BuildUtils.get().model();
    }

    public static String getSerial(){
        return BuildUtils.get().serial();
    }

    public static String getManufacturer(){
        return BuildUtils.get().manufacturer();
    }

    private BuildUtils() {
    }

    public static BuildUtils get() {
        if (singleton == null) {
            synchronized (BuildUtils.class) {
                if (singleton == null) {
                    singleton = new BuildUtils();
                }
            }
        }
        return singleton;
    }

    private String brand;
    private String model;
    private String serial;
    private String manufacturer;

    public String brand() {
        if (TextUtils.isEmpty(brand)) {
            KVDatabase.get("BRAND", s -> {
                brand = s;
                return null;
            });
            if (TextUtils.isEmpty(brand)) {
                brand = Build.BRAND;
                KVDatabase.set("BRAND", brand);
            }
        }
        return brand;
    }

    public String model() {
        if (TextUtils.isEmpty(model)) {
            KVDatabase.get("MODEL", s -> {
                model = s;
                return null;
            });
            if (TextUtils.isEmpty(model)) {
                model = Build.MODEL;
                KVDatabase.set("MODEL", model);
            }
        }
        return model;
    }

    @SuppressLint("HardwareIds")
    public String serial() {
        if (TextUtils.isEmpty(serial)) {
            KVDatabase.get("SERIAL", s -> {
                serial = s;
                return null;
            });
            if (TextUtils.isEmpty(serial)) {
                serial = Build.SERIAL;
                KVDatabase.set("SERIAL", serial);
            }
        }
        return serial;
    }

    public String manufacturer() {
        if (TextUtils.isEmpty(manufacturer)) {
            KVDatabase.get("MANUFACTURER", s -> {
                manufacturer = s;
                return null;
            });
            if (TextUtils.isEmpty(manufacturer)) {
                manufacturer = Build.MANUFACTURER;
                KVDatabase.set("MANUFACTURER", manufacturer);
            }
        }
        return manufacturer;
    }

}
