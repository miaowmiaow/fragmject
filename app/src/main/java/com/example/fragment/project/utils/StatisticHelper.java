package com.example.fragment.project.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Keep;
import androidx.appcompat.widget.SwitchCompat;

import com.example.miaow.base.adapter.BaseAdapter;

import java.util.Objects;

public class StatisticHelper {

    @Keep
    public static void viewOnClick(View view) {
        System.out.println("自动埋点 --> ViewId:" + getViewId(view) + " ViewText:" + getElementContent(view));
    }

    @Keep
    public static void viewOnPageSelected(int position) {
        System.out.println("自动埋点 --> OnPageSelected:" + position);
    }

    @Keep
    public static void testAnnotation(Object object, int code, String message) {
        System.out.println("自动埋点:注解 --> " + message + ":" + code + ":" + object.getClass().getSimpleName());
    }

    @Keep
    public static void viewOnItemClick(BaseAdapter.ViewBindHolder holder, int position) {
        System.out.println("自动埋点 --> onItemChildClick:" + " holderViewId:" + getViewId(holder.itemView) + " holderViewText:" + getElementContent(holder.itemView) + "position:" + position);
    }

    @Keep
    public static void viewOnItemChildClick(View view, BaseAdapter.ViewBindHolder holder, int position) {
        System.out.println("自动埋点 --> onItemChildClick:" + " ViewId:" + getViewId(view) + " ViewText:" + getElementContent(view) + " holderViewId:" + getViewId(holder.itemView) + " holderViewText:" + getElementContent(holder.itemView) + "position:" + position);
    }

    /**
     * 获取 view 的 anroid:id 对应的字符串
     *
     * @param view View
     * @return String
     */
    public static String getViewId(View view) {
        String idString = null;
        try {
            if (view.getId() != View.NO_ID) {
                idString = view.getContext().getResources().getResourceEntryName(view.getId());
            }
        } catch (Exception e) {
            //ignore
        }
        return idString;
    }

    /**
     * 获取 View 上显示的文本
     *
     * @param view View
     * @return String
     */
    public static String getElementContent(View view) {
        if (view == null) {
            return null;
        }

        CharSequence viewText = null;
        if (view instanceof CheckBox checkBox) { // CheckBox
            viewText = checkBox.getText();
        } else if (view instanceof SwitchCompat switchCompat) {
            viewText = switchCompat.getTextOn();
        } else if (view instanceof RadioButton radioButton) { // RadioButton
            viewText = radioButton.getText();
        } else if (view instanceof ToggleButton toggleButton) { // ToggleButton
            boolean isChecked = toggleButton.isChecked();
            if (isChecked) {
                viewText = toggleButton.getTextOn();
            } else {
                viewText = toggleButton.getTextOff();
            }
        } else if (view instanceof Button button) { // Button
            viewText = button.getText();
        } else if (view instanceof CheckedTextView textView) { // CheckedTextView
            viewText = textView.getText();
        } else if (view instanceof TextView textView) { // TextView
            viewText = textView.getText();
        } else if (view instanceof SeekBar seekBar) {
            viewText = String.valueOf(seekBar.getProgress());
        } else if (view instanceof RatingBar ratingBar) {
            viewText = String.valueOf(ratingBar.getRating());
        }
        if (viewText != null) {
            return viewText.toString();
        }
        return null;
    }

    /**
     * 获取 View 所属 Activity
     *
     * @param view View
     * @return Activity
     */
    public static Activity getActivityFromView(View view) {
        Activity activity = null;
        if (view == null) {
            return null;
        }

        try {
            Context context = view.getContext();
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }
            }
        } catch (Exception e) {
            Log.i("getActivityFromView", Objects.requireNonNull(e.getMessage()));
        }
        return activity;
    }

    /**
     * 获取 Android ID
     *
     * @param context Context
     * @return String
     */
    @SuppressLint("HardwareIds")
    public static String getAndroidID(Context context) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            Log.i("getAndroidID", Objects.requireNonNull(e.getMessage()));
        }
        return androidID;
    }

}
