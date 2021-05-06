package com.example.fragment.library.base.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.fragment.library.base.component.provider.BaseProvider;

/**
 * toast显示类，可以在子线程直接调用
 */
public class ToastUtil {

    private static Toast toast;
    private static Handler handler = new Handler(Looper.getMainLooper());

    public static void show(CharSequence text) {
        show(text, Toast.LENGTH_LONG);
    }

    public static void show(final CharSequence text, final int duration) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
			showToast(text, duration);
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
					showToast(text, duration);
                }
            });
        }
    }

    private static void showToast(CharSequence text, int duration) {
        showToast(BaseProvider.mContext, text,  duration);
    }

    private static void showToast(Context context, CharSequence text, int duration) {
        if (toast != null) {
            toast.cancel();
        }
        if(!TextUtils.isEmpty(text)){
            toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

}
