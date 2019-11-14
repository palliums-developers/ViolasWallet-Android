package com.palliums.utils

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import com.palliums.content.ContextProvider
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by elephant on 2019-11-14 09:50.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class StatusBarUtil {

    companion object {
        private var mIsMIUI = false
        private var mIsMeiZu = false
        private var mIsGet = false

        /**
         * 获取状态栏高度
         */
        @JvmStatic
        fun getStatusBarHeight(context: Context = ContextProvider.getContext()): Int {
            var result = DensityUtility.dp2px(context, 24)
            val resId = context.resources.getIdentifier(
                "status_bar_height", "dimen", "android"
            )
            if (resId > 0) {
                result = context.resources.getDimensionPixelOffset(resId)
            }
            return result
        }

        /**
         * 设置状态是否显示浅色模式（深色字体）
         * @param activity
         * @param light 是否为浅色模式
         */
        @JvmStatic
        fun setLightStatusBarMode(activity: Activity, light: Boolean) {
            if (isMIUI()) {
                setStatusBarLightModeForMIUI(activity, light)
            } else if (isFlyme()) {
                setStatusBarLightModeForFlyme(activity, light)
            }

            //6.0 +
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                lightStatusBarMode(activity, light)
            }
        }

        @TargetApi(23)
        private fun lightStatusBarMode(activity: Activity, enable: Boolean) {
            val decorView = activity.window.decorView
            var visibility = decorView.systemUiVisibility

            visibility = if (enable) {
                visibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                visibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }

            decorView.systemUiVisibility = visibility
        }

        /**
         * 设置状态栏字体图标为深色，需要MIUIV6以上
         *
         * @param activity
         * @param dark     是否把状态栏字体及图标颜色设置为深色
         * @return boolean 成功执行返回true
         */
        private fun setStatusBarLightModeForMIUI(activity: Activity, dark: Boolean): Boolean {
            var result = false
            val window = activity.window
            activity.window?.let {
                try {
                    val clazz = window.javaClass
                    val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
                    val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
                    val darkModeFlag = field.getInt(layoutParams)
                    val extraFlagField = clazz.getMethod(
                        "setExtraFlags",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    if (dark) {
                        extraFlagField.invoke(window, darkModeFlag, darkModeFlag)//状态栏透明且黑色字体
                    } else {
                        extraFlagField.invoke(window, 0, darkModeFlag)//清除黑色字体
                    }

                    result = true
                } catch (e: Exception) {
                    // ignore
                }
            }

            return result
        }

        private fun setStatusBarLightModeForFlyme(activity: Activity, dark: Boolean): Boolean {
            var result = false
            activity.window?.let {
                try {
                    val layoutParams = it.attributes
                    val darkFlag = WindowManager.LayoutParams::class.java
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
                    val meizuFlags = WindowManager.LayoutParams::class.java
                        .getDeclaredField("meizuFlags")
                    darkFlag.isAccessible = true
                    meizuFlags.isAccessible = true
                    val bit = darkFlag.getInt(null)
                    var value = meizuFlags.getInt(layoutParams)
                    value = if (dark) {
                        value or bit
                    } else {
                        value and bit.inv()
                    }
                    meizuFlags.setInt(layoutParams, value)
                    it.attributes = layoutParams

                    result = true
                } catch (e: Exception) {
                    // ignore
                }
            }

            return result
        }

        private fun isMIUI(): Boolean {
            if (!mIsGet) {
                val temp = getSystemProperty("ro.miui.ui.version.name")
                if (!TextUtils.isEmpty(temp) && "\n" != temp) {
                    mIsMIUI = true
                }
                mIsMeiZu = "Meizu".equals(Build.MANUFACTURER, ignoreCase = true)

                mIsGet = true
            }

            return mIsMIUI
        }

        private fun isFlyme(): Boolean {
            if (!mIsGet) {
                val temp = getSystemProperty("ro.miui.ui.version.name")
                if (!TextUtils.isEmpty(temp) && "\n" != temp) {
                    mIsMIUI = true
                }
                mIsMeiZu = "Meizu".equals(Build.MANUFACTURER, ignoreCase = true)

                mIsGet = true
            }

            return mIsMeiZu
        }

        private fun getSystemProperty(propName: String): String? {
            var result: String? = null
            try {
                val cmd = if (TextUtils.isEmpty(propName)) {
                    "getprop"
                } else {
                    "getprop $propName"
                }

                val process = Runtime.getRuntime().exec(cmd)

                result = stream2String(process.inputStream)
            } catch (ex: IOException) {
                // ignore
            }

            return result
        }

        @Throws(IOException::class)
        private fun stream2String(`in`: InputStream): String {
            val cs = "utf-8"
            val reader = InputStreamReader(`in`, cs)
            val sb = StringBuilder()
            val buffer = CharArray(10240)
            var count: Int
            while (reader.read(buffer, 0, buffer.size).also { count = it } > 0) {
                sb.append(buffer, 0, count)
            }

            return sb.toString()
        }
    }
}