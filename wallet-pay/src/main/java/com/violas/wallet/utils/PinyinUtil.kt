package com.violas.wallet.utils

import com.github.stuxuhai.jpinyin.PinyinFormat
import com.github.stuxuhai.jpinyin.PinyinHelper

/**
 * Created by elephant on 2019-11-27 17:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

//    String str = "你好世界";
//    PinyinHelper.convertToPinyinString(str, ",", PinyinFormat.WITH_TONE_MARK); // nǐ,hǎo,shì,jiè
//    PinyinHelper.convertToPinyinString(str, ",", PinyinFormat.WITH_TONE_NUMBER); // ni3,hao3,shi4,jie4
//    PinyinHelper.convertToPinyinString(str, ",", PinyinFormat.WITHOUT_TONE); // ni,hao,shi,jie
//    PinyinHelper.getShortPinyin(str); // nhsj
//    PinyinHelper.addPinyinDict("user.dict");  // 添加用户自定义字典
fun chinese2Pinyin(str: String): String {
    try {
        return PinyinHelper.convertToPinyinString(str, "", PinyinFormat.WITHOUT_TONE)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return str
}