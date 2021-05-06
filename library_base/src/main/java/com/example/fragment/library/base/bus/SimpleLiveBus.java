package com.example.fragment.library.base.bus;

import androidx.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;

public class SimpleLiveBus {

    private static final class SingleHolder {
        private static final SimpleLiveBus INSTANCE = new SimpleLiveBus();
    }

    public static SimpleLiveBus get() {
        return SingleHolder.INSTANCE;
    }

    private final ConcurrentHashMap<Object, LiveEvent<Object>> mEventMap;

    private SimpleLiveBus() {
        mEventMap = new ConcurrentHashMap<>();
    }

    public <T> LiveEvent<T> with(@NonNull final String key, @NonNull final Class<T> clazz) {
        return realWith(key, clazz);
    }

    public <T> LiveEvent<T> with(@NonNull final Class<T> clazz) {
        return realWith(null, clazz);
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
