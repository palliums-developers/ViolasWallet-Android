package com.violas.wallet.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by QRoronoa on 0014, 5月14日.
 * Copyright © 2019. All rights reserved.
 * *
 * desc:
 */
public class LightStatusBarUtil {

    /**
     * 设置状态是否显示浅色模式（深色字体）
     * @param activity
     * @param light 是否为浅色模式
     */
    public static void setLightStatusBarMode(Activity activity, boolean light) {
        if (isMIUI()) {
            setStatusBarLightModeForMIUI(activity, light);
        }else if(isFlyme()){
            setStatusBarLightModeForFlyme(activity, light);
        }

        //6.0 +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            lightStatusBarMode(activity,light);
        }
    }

    @TargetApi(23)
    private static void lightStatusBarMode(Activity activity, boolean enable) {
        View decorView = activity.getWindow().getDecorView();
        int visibility = decorView.getSystemUiVisibility();

        if (enable) {
            visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            visibility &= (~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        decorView.setSystemUiVisibility(visibility);
    }


    ///////////////////////////////////////////////////////////////////////////
    // 小米相关
    ///////////////////////////////////////////////////////////////////////////
    /**
     * 设置状态栏字体图标为深色，需要MIUIV6以上
     *
     * @param activity
     * @param dark     是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回true
     */
    private static boolean setStatusBarLightModeForMIUI(Activity activity, boolean dark) {
        boolean result = false;
        Window window = activity.getWindow();
        if (window != null) {
            Class clazz = window.getClass();
            try {
                int darkModeFlag = 0;
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                if (dark) {
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag);//状态栏透明且黑色字体
                } else {
                    extraFlagField.invoke(window, 0, darkModeFlag);//清除黑色字体
                }
                result = true;
            } catch (Exception e) {
            }
        }
        return result;
    }

    private static boolean setStatusBarLightModeForFlyme(Activity activity, boolean isFontColorDark) {
        Window window = activity.getWindow();
        boolean result = false;
        if (window != null) {
            try {
                WindowManager.LayoutParams lp = window.getAttributes();
                Field darkFlag = WindowManager.LayoutParams.class
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class
                        .getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                meizuFlags.setAccessible(true);
                int bit = darkFlag.getInt(null);
                int value = meizuFlags.getInt(lp);
                if (isFontColorDark) {
                    value |= bit;
                } else {
                    value &= ~bit;
                }
                meizuFlags.setInt(lp, value);
                window.setAttributes(lp);
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    private static boolean mIsMIUI;
    private static boolean mIsMeiZu;
    private static boolean mIsGet;

    public static boolean isMIUI() {
        if (!mIsGet) {
            String temp = getSystemProperty("ro.miui.ui.version.name");
            if (!TextUtils.isEmpty(temp) && !"\n".equals(temp)) {
                mIsMIUI = true;
            }
            mIsMeiZu = "Meizu".equalsIgnoreCase(Build.MANUFACTURER);

            mIsGet = true;
        }

        return mIsMIUI;
    }

    public static String getSystemProperty(String propName) {
        String result = null;
        try {
            String cmd;
            if (TextUtils.isEmpty(propName)) {
                cmd = "getprop";
            } else {
                cmd = "getprop " + propName;
            }

            Process p = Runtime.getRuntime().exec(cmd);
            result = stream2String(p.getInputStream());
        } catch (IOException ex) {
        }

        return result;
    }


    public static boolean isFlyme() {
        if (!mIsGet) {
            String temp = getSystemProperty("ro.miui.ui.version.name");
            if (!TextUtils.isEmpty(temp) && !"\n".equals(temp)) {
                mIsMIUI = true;
            }
            mIsMeiZu = "Meizu".equalsIgnoreCase(Build.MANUFACTURER);

            mIsGet = true;
        }
        return mIsMeiZu;
    }

    private static String stream2String(InputStream in) throws IOException {
        String cs = "utf-8";
        InputStreamReader reader = new InputStreamReader(in, cs);
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[10240];
        int count = 0;

        while ((count = reader.read(buffer, 0, buffer.length)) > 0) {
            sb.append(buffer, 0, count);
        }

        return sb.toString();
    }

}
