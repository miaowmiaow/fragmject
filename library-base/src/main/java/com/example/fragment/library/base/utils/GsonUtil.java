package com.example.fragment.library.base.utils;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Gson解析工具类
 */
public class GsonUtil {

    public static String getString(Object object) {
        Gson gson = buildGson();
        String gsonString = gson.toJson(object);
        return gsonString;
    }

    public static <T> T getBean(String key, JSONObject json, Class<T> clz) {
        Gson gson = buildGson();
        T t = null;
        try {
            t = gson.fromJson(json.getString(key), clz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    public static <T> T getBean(String json, Class<T> clz) {
        Gson gson = buildGson();
        T t = null;
        try {
            t = gson.fromJson(json, clz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    public static <T> T getBean(JSONObject json, Class<T> clz) {
        Gson gson = buildGson();
        T t = null;
        try {
            t = gson.fromJson(json.toString(), clz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    public static JSONObject getJSONObjectFromMapNom(Map<String, Object> params) {
        Gson gson = buildGson();
        String jsonStr = gson.toJson(params);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static JSONObject getJSONObjectFromString(String params) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static JSONObject getJSONObjectFromMap(Map<String, String> params) {
        Gson gson = buildGson();
        String jsonStr = gson.toJson(params);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static <T> List<T> getBeanList(String json, TypeToken<List<T>> typeToken) {
        Gson gson = buildGson();
        List<T> t = null;
        try {
            t = gson.fromJson(json, typeToken.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    public static <T> List<T> getBeanList(JSONObject json, TypeToken<List<T>> typeToken) {
        Gson gson = buildGson();
        List<T> t = null;
        try {
            t = gson.fromJson(json.toString(), typeToken.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    /**
     * @return
     */
    private static Gson buildGson() {
        GsonBuilder gsonBulder = new GsonBuilder();
        gsonBulder
                .registerTypeAdapter(String.class, STRING)
                .registerTypeAdapter(Integer.class, INTEGER)
                .registerTypeAdapter(Double.class, DOUBLE)
                .registerTypeAdapter(Long.class, LONG);
        gsonBulder.serializeNulls();
        return gsonBulder.create();
    }

    /**
     * 自定义TypeAdapter ,null对象将被解析成空字符串
     */
    private static final TypeAdapter<String> STRING = new TypeAdapter<String>() {
        public String read(JsonReader reader) {
            try {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return ""; // 原先是返回null，这里改为返回空字符串
                }
                return reader.nextString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        public void write(JsonWriter writer, String value) {
            try {
                if (value == null) {
                    writer.value("");
                    return;
                }
                writer.value(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * 自定义TypeAdapter ,null对象将被解析成0
     */
    private static final TypeAdapter<Integer> INTEGER = new TypeAdapter<Integer>() {
        public Integer read(JsonReader reader) {
            try {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return 0; // 原先是返回null，这里改为返回0
                }
                return reader.nextInt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public void write(JsonWriter writer, Integer value) {
            try {
                if (value == null) {
                    writer.value(0);
                    return;
                }
                writer.value(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * 自定义TypeAdapter ,null对象将被解析成0.0
     */
    private static final TypeAdapter<Double> DOUBLE = new TypeAdapter<Double>() {
        public Double read(JsonReader reader) {
            try {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return 0.0; // 原先是返回null，这里改为返回0
                }
                return reader.nextDouble();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0.0;
        }

        public void write(JsonWriter writer, Double value) {
            try {
                if (value == null) {
                    writer.value(0.0);
                    return;
                }
                writer.value(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * 自定义TypeAdapter ,null对象将被解析成0L
     */
    private static final TypeAdapter<Long> LONG = new TypeAdapter<Long>() {
        public Long read(JsonReader reader) {
            try {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return 0L; // 原先是返回null，这里改为返回0
                }
                return reader.nextLong();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0L;
        }

        public void write(JsonWriter writer, Long value) {
            try {
                if (value == null) {
                    writer.value(0L);
                    return;
                }
                writer.value(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /***
     *
     * 获取JSON类型
     *         判断规则
     *             判断第一个字母是否为{或[ 如果都不是则不是一个JSON格式的文本
     *
     * @param str
     * @return 0不是JSON格式的字符串 2JSONObject 1JSONArray
     */
    public static int getJSONType(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }

        final char[] strChar = str.substring(0, 1).toCharArray();
        final char firstChar = strChar[0];

        if (firstChar == '{') {
            return 2;
        } else if (firstChar == '[') {
            return 1;
        } else {
            return 0;
        }
    }


}
