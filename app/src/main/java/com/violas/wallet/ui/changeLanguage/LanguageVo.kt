package com.violas.wallet.ui.changeLanguage

import java.util.*

data class LanguageVo(
    var select: Boolean,
    val type: Int,
    val locale: Locale,
    val res: String,
    val resmore: String
)