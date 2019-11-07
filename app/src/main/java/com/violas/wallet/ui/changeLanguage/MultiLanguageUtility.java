package com.violas.wallet.ui.changeLanguage;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import com.violas.wallet.R;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 多语言切换的帮助类
 * http://blog.csdn.net/finddreams
 */
public class MultiLanguageUtility {

    private static final Map<Integer, LanguageBean> mLocaleLanguageMap = new LinkedHashMap<>();

    static {
        mLocaleLanguageMap.put(LanguageType.LANGUAGE_CHINESE_SIMPLIFIED, new LanguageBean(LanguageType.LANGUAGE_CHINESE_SIMPLIFIED, Locale.SIMPLIFIED_CHINESE, R.string.language_chinese_simplified, R.string.language_chinese_simplified_more));
        mLocaleLanguageMap.put(LanguageType.LANGUAGE_EN, new LanguageBean(LanguageType.LANGUAGE_EN, Locale.ENGLISH, R.string.language_en, R.string.language_en_more));
        mLocaleLanguageMap.put(LanguageType.LANGUAGE_CHINESE_TRADITIONAL, new LanguageBean(LanguageType.LANGUAGE_CHINESE_TRADITIONAL, Locale.TRADITIONAL_CHINESE, R.string.language_chinese_traditional, R.string.language_chinese_traditional_more));
    }

    private static final String TAG = "MultiLanguageUtil";
    private static MultiLanguageUtility instance;
    private Context mContext;
    public static final String SAVE_LANGUAGE = "save_language";

    public static void init(Context mContext) {
        if (instance == null) {
            synchronized (MultiLanguageUtility.class) {
                if (instance == null) {
                    instance = new MultiLanguageUtility(mContext);
                }
            }
        }
    }

    public static MultiLanguageUtility getInstance() {
        if (instance == null) {
            throw new IllegalStateException("You must be init MultiLanguageUtil first");
        }
        return instance;
    }

    private MultiLanguageUtility(Context context) {
        this.mContext = context;
    }

    public Map<Integer, LanguageBean> getSupportLanguage() {
        return mLocaleLanguageMap;
    }

    /**
     * 设置语言
     */
    public void setConfiguration() {
        Locale targetLocale = getLanguageLocale();
        Configuration configuration = mContext.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(targetLocale);
        } else {
            configuration.locale = targetLocale;
        }
        Resources resources = mContext.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, dm);//语言更换生效的代码!
    }

    /**
     * 如果不是英文、简体中文、繁体中文，默认返回繁体中文
     *
     * @return
     */
    private Locale getLanguageLocale() {
        int languageType = getSaveLanguageType();
        Locale locale;
        LanguageBean languageBean = mLocaleLanguageMap.get(languageType);
        if (languageBean == null) {
            locale = getSysLocale();
        } else {
            locale = languageBean.getLocale();
        }
        return locale;
    }

    private String getSystemLanguage(Locale locale) {
        return locale.getLanguage() + "_" + locale.getCountry();

    }

    //以上获取方式需要特殊处理一下
    public static Locale getSysLocale() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            locale = Resources.getSystem().getConfiguration().locale;
        }

        return locale;
    }

    /**
     * 更新语言
     *
     * @param languageType
     */
    public void updateLanguage(int languageType) {
        LanguageShared.getInstance(mContext).putInt(MultiLanguageUtility.SAVE_LANGUAGE, languageType);
        MultiLanguageUtility.getInstance().setConfiguration();
    }

    public void notification() {
        //todo Eventbus 通知
    }

    /**
     * 获取到用户保存的语言类型
     *
     * @return
     */
    public int getSaveLanguageType() {
        return LanguageShared.getInstance(mContext).getInt(MultiLanguageUtility.SAVE_LANGUAGE, LanguageType.LANGUAGE_CHINESE_TRADITIONAL);
    }

    public static Context attachBaseContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return createConfigurationResources(context);
        } else {
            MultiLanguageUtility.getInstance().setConfiguration();
            return context;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context createConfigurationResources(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = getInstance().getLanguageLocale();
        configuration.setLocale(locale);
        DisplayMetrics dm = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, dm);
        return context.createConfigurationContext(configuration);
    }

    @NotNull
    public String getLocalTag() {
        switch (getSaveLanguageType()) {
            case LanguageType.LANGUAGE_CHINESE_SIMPLIFIED:
                return "CN";
            case LanguageType.LANGUAGE_EN:
                return "EN";
            default:
                return "CNT";
        }

    }
}
