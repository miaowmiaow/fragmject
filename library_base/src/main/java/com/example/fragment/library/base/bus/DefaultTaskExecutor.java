package com.example.fragment.library.base.bus;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultTaskExecutor {

    private static volatile DefaultTaskExecutor sInstance;

    private DefaultTaskExecutor() {}
    
    public static DefaultTaskExecutor getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (DefaultTaskExecutor.class) {
            if (sInstance == null) {
                sInstance = new DefaultTaskExecutor();
            }
        }
        return sInstance;
    }

    private final Object mLock = new Object();

    private final ExecutorService mDiskIO = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private static final String THREAD_NAME_STEM = "live_event_bus_disk_io_%d";

        private final AtomicInteger mThreadId = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(String.format(Locale.CHINA, THREAD_NAME_STEM, mThreadId.getAndIncrement()));
            return t;
        }
    });

    @Nullable
    private volatile Handler mMainHandler;

    public void executeOnDiskIO(Runnable runnable) {
        mDiskIO.execute(runnable);
    }

    public void postToMainThread(Runnable runnable) {
        if (mMainHandler == null) {
            synchronized (mLock) {
                if (mMainHandler == null) {
                    mMainHandler = createAsync(Looper.getMainLooper());
                }
            }
        }
        //noinspection ConstantConditions
        mMainHandler.post(runnable);
    }

    public boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private static Handler createAsync(@NonNull Looper looper) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Handler.createAsync(looper);
        }
        return new Handler(looper);
    }
}
