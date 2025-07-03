package com.android.ledcontorl.Utils;

import android.util.Log;

import java.lang.reflect.Method;

/**
 *
 * 通过反射机制来set ,get 系统属性,
 * 2025年5月29日 D.K Mo
 */

public class SystemPropertiesUtils {

    private final static String TAG = "SystemProperties";

    public static String get(String key) {
        Class<?> SysProp = null;
        Method method = null;
        String value = null;
        try {
            SysProp = Class.forName("android.os.SystemProperties");
            method = SysProp.getMethod("get", String.class);
            value = (String) method.invoke(null, key);
            Log.i(TAG, "value：" + value);
        } catch (Exception e) {
            Log.e(TAG, "read SystemProperties error", e);
        }
        return value;
    }

    public static String get(String key, String defaultValue) {
        Class<?> SysProp = null;
        Method method = null;
        String value = null;
        try {
            SysProp = Class.forName("android.os.SystemProperties");
            method = SysProp.getMethod("get", String.class, String.class);
            value = (String) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "read SystemProperties error", e);
        }
        return value;
    }

    public static int getInt(String key, int defaultValue) {
        Class<?> SysProp = null;
        Method method = null;
        int value = 0;
        try {
            SysProp = Class.forName("android.os.SystemProperties");
            method = SysProp.getMethod("getInt", String.class, int.class);
            value = (Integer) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "read SystemProperties error", e);
        }
        return value;
    }

    public static long getLong(String key, long defaultValue) {
        Class<?> SysProp = null;
        Method method = null;
        long value = 0;
        try {
            SysProp = Class.forName("android.os.SystemProperties");
            method = SysProp.getMethod("getLong", String.class, long.class);
            value = (Long) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "read SystemProperties error", e);
        }
        return value;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        Class<?> SysProp = null;
        Method method = null;
        boolean value = false;
        try {
            SysProp = Class.forName("android.os.SystemProperties");
            method = SysProp.getMethod("getBoolean", String.class, boolean.class);
            value = (Boolean) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "read SystemProperties error", e);
        }
        return value;
    }

    public static void set(String key, String value) {
        Class<?> SysProp = null;
        Method method = null;
        try {
            SysProp = Class.forName("android.os.SystemProperties");
            method = SysProp.getMethod("set", String.class, String.class);
            method.invoke(null, key, value);
        } catch (Exception e) {
            Log.e(TAG, "read SystemProperties error", e);
        }
    }

    public static void addChangeCallback(Runnable runnable) {
        Class<?> SysProp = null;
        Method method = null;
        try {
            SysProp = Class.forName("android.os.SystemProperties");
            method = SysProp.getMethod("addChangeCallback", Runnable.class);
            method.invoke(null, runnable);
        } catch (Exception e) {
            Log.e(TAG, "read SystemProperties error", e);
        }
    }
}