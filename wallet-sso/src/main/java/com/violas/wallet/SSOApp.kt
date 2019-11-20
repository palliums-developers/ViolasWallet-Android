package com.violas.wallet

import android.content.Context
import com.palliums.content.App
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility

class SSOApp : App() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        MultiLanguageUtility.init(newBase)
        MultiLanguageUtility.attachBaseContext(newBase)
    }
}