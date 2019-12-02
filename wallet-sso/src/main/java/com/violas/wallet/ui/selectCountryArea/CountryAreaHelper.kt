package com.violas.wallet.ui.selectCountryArea

import android.text.TextUtils
import com.palliums.content.ContextProvider
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.utils.hanzi2Pinyin
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

/**
 * Created by elephant on 2019-11-27 16:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun isChinaMainland(countryAreaVO: CountryAreaVO): Boolean {
    return countryAreaVO.areaCode == "86"
}

fun getGroupedCountryAreas(): LinkedHashMap<String, List<CountryAreaVO>> {
    val assetsFileName = getCountryAreaAssetsFileName()
    val countryAreaJsonData = getCountryAreaJsonData(assetsFileName)

    val keys = ArrayList<String>()
    val data = LinkedHashMap<String, List<CountryAreaVO>>()
    var temp = ArrayList<CountryAreaVO>()

    // 如果是简体中文,繁体中文,取出首字母进行分组(其他国家已经按照字母排序),
    // 注意,在json文件里,所有国家地区都是排序好的,如果新添加国家也要排序好,为了提高加载效率
    val need2Pinyin = assetsFileName.endsWith("zh.json")

    for (i in 0 until countryAreaJsonData.length()) {
        val jsonObject: JSONObject = countryAreaJsonData.optJSONObject(i)

        val areaCode = jsonObject.optString("areaCode")
        val countryName = jsonObject.optString("countryName")
        val countryCode = jsonObject.optString("countryCode")

        val countryAreaVO = CountryAreaVO(areaCode, countryName, countryCode)

        // 快速索引关键字
        var key = countryName.substring(0, 1)
        if (need2Pinyin) {
            key = hanzi2Pinyin(key).substring(0, 1)
        }
        key = key.toUpperCase(Locale.ENGLISH)

        // 当第一次,或者当前字母不跟上次相同,说明新的一组开始
        if (keys.size == 0 || !TextUtils.equals(keys[keys.size - 1], key)) {
            keys.add(key)
            temp = ArrayList()
            data[key] = temp
        }

        countryAreaVO.setGroupName(key)
        temp.add(countryAreaVO)
    }

    return data
}

fun getCountryArea(defaultCountryCode: String? = null): CountryAreaVO {
    val countryAreaJsonData = getCountryAreaJsonData()

    val locale = MultiLanguageUtility.getInstance().languageLocale
    val localCountryCode = locale.country

    for (i in 0 until countryAreaJsonData.length()) {

        val jsonObject = countryAreaJsonData.optJSONObject(i)

        if (jsonObject != null) {

            val areaCode = jsonObject.optString("areaCode")
            val countryName = jsonObject.optString("countryName")
            val countryCode = jsonObject.optString("countryCode")

            if (defaultCountryCode.equals(countryCode, ignoreCase = true)
                || localCountryCode.equals(countryCode, ignoreCase = true)
            ) {
                return CountryAreaVO(areaCode, countryName, countryCode)
            }
        }
    }

    return CountryAreaVO(
        areaCode = "65",
        countryName = if (locale.language == "zh") {
            "新加坡"
        } else {
            "Singapore"
        },
        countryCode = "SG"
    )
}

fun getCountryAreaJsonData(
    assetsFileName: String = getCountryAreaAssetsFileName()
): JSONArray {

    var inputStream: InputStream? = null
    try {

        inputStream = ContextProvider.getContext().assets.open(assetsFileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }

        return JSONArray(stringBuilder.toString())
    } catch (e: Exception) {
        e.printStackTrace()

        return JSONArray()
    } finally {
        try {
            inputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun getCountryAreaAssetsFileName(): String {
    val locale = MultiLanguageUtility.getInstance().languageLocale
    val localeLanguage = if (locale.language == "zh") {
        "zh"
    } else {
        "en"
    }
    return "country_$localeLanguage.json"
}