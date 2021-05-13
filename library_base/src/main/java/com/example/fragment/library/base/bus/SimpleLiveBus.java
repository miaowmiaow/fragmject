package com.example.fragment.library.base.bus;

import androidx.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * LiveData 只会将更新通知给活跃的观察者，
 * 所以观察者生命周期要处于 STARTED 或 RESUMED 状态。
 */
public class SimpleLiveBus {

    private static final class SingleHolder {
        private static final SimpleLiveBus INSTANCE = new SimpleLiveBus();
    }

    private final ConcurrentHashMap<Object, LiveEvent<Object>> mEventMap;

    private SimpleLiveBus() {
        mEventMap = new ConcurrentHashMap<>();
    }

    public static <T> LiveEvent<T> with(@NonNull final String key) {
        return SingleHolder.INSTANCE.realWith(key, null);
    }

    public static <T> LiveEvent<T> with(@NonNull final Class<T> clazz) {
        return SingleHolder.INSTANCE.realWith(null, clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> LiveEvent<T> realWith(final String key, final Class<T> clazz) {
        final Object objectKey;
        if (key != null) {
            objectKey = key;
        } else if (clazz != null) {
            objectKey = clazz;
        } else {
            throw new IllegalArgumentException("key and clazz, one of which must not be null");
        }
        LiveEvent<Object> result = mEventMap.get(objectKey);
        if (result != null) return (LiveEvent<T>) result;
        synchronized (mEventMap) {
            result = mEventMap.get(objectKey);
            if (result == null) {
                result = new LiveEvent<>();
                mEventMap.put(objectKey, result);
            }
        }
        return (LiveEvent<T>) result;
    }
}
