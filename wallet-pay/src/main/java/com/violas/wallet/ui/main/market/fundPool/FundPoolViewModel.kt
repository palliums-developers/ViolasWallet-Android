package com.violas.wallet.ui.main.market.fundPool

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import kotlinx.coroutines.delay

/**
 * Created by elephant on 2020/6/30 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class FundPoolViewModel : BaseViewModel() {

    companion object {
        const val ACTION_GET_TOKEN_PAIRS = 0x01
    }

    // 当前的操作模式，分转入和转出
    private val currOpModeLiveData = MutableLiveData<FundPoolOpMode>(FundPoolOpMode.TransferIn)

    // 转入模式下选择的通证
    private val currFirstTokenLiveData = MediatorLiveData<String?>()
    private val currSecondTokenLiveData = MediatorLiveData<String?>()

    // 转出模式下选择的交易对和可转出的交易对列表
    private val currTokenPairLiveData = MediatorLiveData<Pair<String, String>?>()
    private val tokenPairsLiveData = MutableLiveData<List<Pair<String, String>>>()
    private val displayTokenPairsLiveData = MediatorLiveData<MutableList<String>>()

    // 兑换率和池份额
    private val exchangeRateLiveData = MediatorLiveData<String>()
    private val shareOfPoolLiveData = MutableLiveData<String>()

    init {
        currFirstTokenLiveData.addSource(currOpModeLiveData) {
            if (it == FundPoolOpMode.TransferIn) {
                currFirstTokenLiveData.postValue(null)
            }
        }

        currSecondTokenLiveData.addSource(currOpModeLiveData) {
            if (it == FundPoolOpMode.TransferIn) {
                currSecondTokenLiveData.postValue(null)
            }
        }

        currTokenPairLiveData.addSource(currOpModeLiveData) {
            if (it == FundPoolOpMode.TransferOut) {
                currTokenPairLiveData.postValue(null)
            }
        }

        displayTokenPairsLiveData.addSource(tokenPairsLiveData) {
            val disPlayList = mutableListOf<String>()
            it.forEach { item ->
                disPlayList.add("${item.first}/${item.second}")
            }
            displayTokenPairsLiveData.postValue(disPlayList)
        }
    }

    //*********************************** 操作模式相关方法 ***********************************//
    fun getCurrOpModeLiveData(): LiveData<FundPoolOpMode> {
        return currOpModeLiveData
    }

    fun getCurrOpModelPosition(): Int {
        return currOpModeLiveData.value?.ordinal ?: -1
    }

    fun switchOpModel(target: FundPoolOpMode) {
        if (target != currOpModeLiveData.value) {
            currOpModeLiveData.postValue(target)
        }
    }

    fun isTransferInMode(): Boolean {
        return currOpModeLiveData.value == FundPoolOpMode.TransferIn
    }

    //*********************************** 转入模式下相关方法 ***********************************//
    fun getCurrFirstTokenLiveData(): LiveData<String?> {
        return currFirstTokenLiveData
    }

    fun getCurrSecondTokenLiveData(): LiveData<String?> {
        return currSecondTokenLiveData
    }

    fun selectToken(selectFirst: Boolean, selected: String) {
        if (selectFirst) {
            if (selected != currFirstTokenLiveData.value) {
                currFirstTokenLiveData.postValue(selected)
            }
        } else {
            if (selected != currSecondTokenLiveData.value) {
                currSecondTokenLiveData.postValue(selected)
            }
        }
    }

    //*********************************** 转出模式下相关方法 ***********************************//
    fun getCurrTokenPairLiveData(): LiveData<Pair<String, String>?> {
        return currTokenPairLiveData
    }

    fun getDisplayTokenPairsLiveData(): MutableLiveData<MutableList<String>> {
        return displayTokenPairsLiveData
    }

    fun getCurrTokenPairPosition(): Int {
        val curr = currTokenPairLiveData.value ?: return -1
        val list = tokenPairsLiveData.value ?: return -1
        list.forEachIndexed { index, item ->
            if (curr.first == item.first && curr.second == item.second) {
                return index
            }
        }
        return -1
    }

    fun selectTokenPair(selectedPosition: Int, currPosition: Int = getCurrTokenPairPosition()) {
        if (selectedPosition != currPosition) {
            val list = tokenPairsLiveData.value ?: return
            if (selectedPosition < 0 || selectedPosition >= list.size) return
            currTokenPairLiveData.postValue(list[selectedPosition])
        }
    }

    //*********************************** 其它信息相关方法 ***********************************//
    fun getExchangeRateLiveData(): LiveData<String> {
        return exchangeRateLiveData
    }

    fun getShareOfPoolLiveData(): LiveData<String> {
        return shareOfPoolLiveData
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        if (action == ACTION_GET_TOKEN_PAIRS) {
            // test code
            delay(500)
            val list =
                mutableListOf(Pair("LBR", "VLS"), Pair("BTC", "VLS"))
            tokenPairsLiveData.postValue(list)
            return
        }
    }

}