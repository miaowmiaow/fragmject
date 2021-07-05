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

import com.example.fragment.library.base.component.adapter.BaseAdapter;

public class StatisticHelper {

    @Keep
    public static void viewOnClick(View view) {
        Log.i("----------自动埋点", " ViewId:" + getViewId(view) + " ViewText:" + getElementContent(view));
    }

    @Keep
    public static void viewOnPageSelected(int position) {
        Log.i("----------自动埋点", " OnPageSelected:" + position);
    }

    @Keep
    public static void testAnnotation(String message, boolean sb) {
        Log.i("----------自动埋点:注解", message + ":" + sb);
    }

    @Keep
    public static void viewOnItemClick(BaseAdapter.ViewBindHolder holder, int position){
        Log.i("----------自动埋点", "onItemChildClick:"+" holderViewId:" + getViewId(holder.itemView)+ " holderViewText:" + getElementContent(holder.itemView)+"position:"+position );
    }
    @Keep
    public static void viewOnItemChildClick(View view, BaseAdapter.ViewBindHolder holder, int position){
        Log.i("----------自动埋点", "onItemChildClick:"+" ViewId:" + getViewId(view)+ " ViewText:" + getElementContent(view)+" holderViewId:" + getViewId(holder.itemView)+ " holderViewText:" + getElementContent(holder.itemView)+"position:"+position );
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
        if (view instanceof CheckBox) { // CheckBox
            CheckBox checkBox = (CheckBox) view;
            viewText = checkBox.getText();
        } else if (view instanceof SwitchCompat) {
            SwitchCompat switchCompat = (SwitchCompat) view;
            viewText = switchCompat.getTextOn();
        } else if (view instanceof RadioButton) { // RadioButton
            RadioButton radioButton = (RadioButton) view;
            viewText = radioButton.getText();
        } else if (view instanceof ToggleButton) { // ToggleButton
            ToggleButton toggleButton = (ToggleButton) view;
            boolean isChecked = toggleButton.isChecked();
            if (isChecked) {
                viewText = toggleButton.getTextOn();
            } else {
                viewText = toggleButton.getTextOff();
            }
        } else if (view instanceof Button) { // Button
            Button button = (Button) view;
            viewText = button.getText();
        } else if (view instanceof CheckedTextView) { // CheckedTextView
            CheckedTextView textView = (CheckedTextView) view;
            viewText = textView.getText();
        } else if (view instanceof TextView) { // TextView
            TextView textView = (TextView) view;
            viewText = textView.getText();
        } else if (view instanceof SeekBar) {
            SeekBar seekBar = (SeekBar) view;
            viewText = String.valueOf(seekBar.getProgress());
        } else if (view instanceof RatingBar) {
            RatingBar ratingBar = (RatingBar) view;
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return androidID;
    }

}
