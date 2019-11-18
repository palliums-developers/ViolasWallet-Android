package com.violas.wallet.ui.changeLanguage

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.content.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChangeLanguageViewModel(val context: Application) : AndroidViewModel(context) {
    val mLanguageList = MutableLiveData<List<LanguageVo>>(arrayListOf())
    val selectKey = MultiLanguageUtility.getInstance().saveLanguageType
    var newSelectKey = selectKey

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val unitList = ArrayList<LanguageVo>()
            MultiLanguageUtility.getInstance().supportLanguage.values.forEach {
                unitList.add(
                    LanguageVo(
                        selectKey == it.type,
                        it.type,
                        it.locale,
                        context.getString(it.res),
                        context.getString(it.resmore)
                    )
                )
            }
            mLanguageList.postValue(unitList)
        }
    }

    fun saveCurrentLanguage(key: Int) {
        viewModelScope.launch {
            MultiLanguageUtility.getInstance().updateLanguage(key)
            newSelectKey = key
        }
    }

    fun finish() {
        viewModelScope.launch {
            if (selectKey != newSelectKey) {
                MultiLanguageUtility.getInstance().notification()
                App.activityStore.forEach {
                    if (it::class.java == ChangeLanguageActivity::class.java) {
                        return@forEach
                    }
                    val intent = it.intent
                    it.overridePendingTransition(0, 0)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    it.finish()
                    it.overridePendingTransition(0, 0)
                    it.startActivity(intent)
                }
            }
        }
    }
}
