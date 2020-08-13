package com.violas.wallet.ui.transfer

import androidx.lifecycle.*
import com.palliums.content.ContextProvider
import com.palliums.utils.getString
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.Exception
import java.math.BigDecimal
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MultiTransferViewModel : ViewModel() {
    companion object {
        @JvmStatic
        val DEF_FEE_PROGRESS = 35
    }

    private val mTransferManager by lazy {
        TransferManager()
    }

    private val mTransactionManager by lazy {
        val identityByCoinType = AccountManager().getIdentityByCoinType(
            if (Vm.TestNet) {
                CoinTypes.BitcoinTest.coinType()
            } else {
                CoinTypes.Bitcoin.coinType()
            }
        )
        val addressList = if (identityByCoinType == null) {
            arrayListOf()
        } else {
            arrayListOf(identityByCoinType.address)
        }
        TransactionManager(addressList)
    }

    val mCurrAssets = MutableLiveData<AssetsVo>()
    val mTransferAmount = MutableLiveData<String>()
    val mTransferPayeeAddress = MutableLiveData<String>()
    val mFeeProgressAmount = MutableLiveData(DEF_FEE_PROGRESS)

    // Pair<String,String> first is Amount,second is unit
    val mFeeAmount = MediatorLiveData<Pair<String, String>>()

    init {
        mFeeAmount.addSource(mCurrAssets) { calculateFee() }
        mFeeAmount.addSource(mTransferAmount) { calculateFee() }
        mFeeAmount.addSource(mFeeProgressAmount) { calculateFee() }

        // 单独监听比特币手续费计算
        viewModelScope.launch(Dispatchers.IO) {
            mTransactionManager.setFeeCallback {
                if (mCurrAssets.value?.isBitcoin() == true) {
                    setFeeAmount(it, mCurrAssets.value?.amountWithUnit?.unit)
                }
            }
        }
    }

    private fun setFeeAmount(amount: String? = null, unit: String? = null) {
        mFeeAmount.postValue(
            Pair(
                amount ?: "0.00",
                unit ?: "unknown"
            )
        )
    }

    private fun calculateFee() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (mCurrAssets.value == null) {
                    setFeeAmount("- -")
                    return@launch
                }
                if (mTransferAmount.value?.isEmpty() == true
                    || (mTransferAmount.value?.toDouble() ?: 0.0) == 0.0
                ) {
                    setFeeAmount("0.00", mCurrAssets.value?.amountWithUnit?.unit)
                    return@launch
                }
                if (mCurrAssets.value?.isBitcoin() == true) {
                    mTransactionManager.transferAmountIntent(
                        mTransferAmount.value?.toDouble() ?: 0.0,
                        mFeeProgressAmount.value ?: DEF_FEE_PROGRESS
                    )
                } else {
                    setFeeAmount("0.00", mCurrAssets.value?.amountWithUnit?.unit)
                }
            } catch (e: Exception) {
                setFeeAmount("- -")
            }
        }
    }

    /**
     * 检查转账的金额，地址是否合理合法
     */
    fun checkConditions(
        account: AccountDO,
        mCurrAssetsAmount: BigDecimal
    ) {
        if (mCurrAssetsAmount < BigDecimal(mTransferAmount.value ?: "0")) {
            throw LackOfBalanceException()
        }
        mTransferManager.checkTransferParam(
            mTransferAmount.value ?: "0",
            mTransferPayeeAddress.value ?: "",
            account
        )
    }

    @Throws(AddressFaultException::class, WrongPasswordException::class)
    suspend fun transfer(
        account: AccountDO,
        pwd: ByteArray
    ) {
        val assets = mCurrAssets.value
            ?: throw RuntimeException(getString(R.string.hint_please_select_currency_transfer))

        suspendCancellableCoroutine<String> { cancellation ->
            cancellation.invokeOnCancellation {
                throw ForcedStopException()
            }

            if (assets.isBitcoin()) {
                val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
                    .decrypt(pwd, account.privateKey) ?: throw WrongPasswordException()

                mTransferManager.transferBtc(
                    transactionManager = mTransactionManager,
                    address = mTransferPayeeAddress.value ?: "",
                    amount = mTransferAmount.value?.toDouble() ?: 0.0,
                    privateKey = privateKey,
                    accountDO = account,
                    progress = mFeeProgressAmount.value ?: DEF_FEE_PROGRESS,
                    success = {
                        cancellation.resume(it)
                    }, error = {
                        cancellation.resumeWithException(it)
                    }
                )
            } else {
                val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
                    .decrypt(pwd, account.privateKey) ?: throw WrongPasswordException()

                viewModelScope.launch(Dispatchers.IO) {
                    mTransferManager.transfer(
                        context = ContextProvider.getContext(),
                        address = mTransferPayeeAddress.value ?: "",
                        amountStr = mTransferAmount.value ?: "",
                        privateKey = privateKey,
                        accountDO = account,
                        progress = mFeeProgressAmount.value ?: DEF_FEE_PROGRESS,
                        token = true,
                        tokenId = assets.getId(),
                        success = {
                            cancellation.resume(it)
                        }, error = {
                            cancellation.resumeWithException(it)
                        }
                    )
                }
            }
        }
    }
}