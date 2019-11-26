package com.violas.wallet.ui.selectCurrency

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.violas.wallet.ui.selectCurrency.bean.CurrencyBean
import java.io.InputStream
import java.io.InputStreamReader


object CurrencyFactory {

    fun parseCurrency(inputStream: InputStream): List<CurrencyBean> {

        return Gson().fromJson<List<CurrencyBean>>(
            InputStreamReader(inputStream),
            object : TypeToken<List<CurrencyBean>>() {}.type
        )
    }
}