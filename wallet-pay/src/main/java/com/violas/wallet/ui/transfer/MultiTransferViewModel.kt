package com.violas.wallet.ui.transfer

import androidx.lifecycle.*
import com.palliums.content.ContextProvider
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.*
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.viewModel.bean.AssetVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
        val bitcoinAccount = AccountManager.getAccountByCoinNumber(
            getBitcoinCoinType().coinNumber()
        )
        val addressList = if (bitcoinAccount == null) {
            arrayListOf()
        } else {
            arrayListOf(bitcoinAccount.address)
        }
        TransactionManager(addressList)
    }

    val mAssetLiveData = MutableLiveData<AssetVo>()
    val mAmountLiveData = MutableLiveData<String>()
    val mFeeProgressLiveData = MutableLiveData(DEF_FEE_PROGRESS)
    val mPayeeAddressLiveData = MutableLiveData<String>()

    // Pair<String,String> first is Amount,second is unit
    val mFeeAmount = MediatorLiveData<Pair<String, String>>()

    init {
        mFeeAmount.addSource(mAssetLiveData) { calculateFee() }
        mFeeAmount.addSource(mAmountLiveData) { calculateFee() }
        mFeeAmount.addSource(mFeeProgressLiveData) { calculateFee() }

        // 单独监听比特币手续费计算
        viewModelScope.launch(Dispatchers.IO) {
            mTransactionManager.setFeeCallback {
                if (mAssetLiveData.value?.isBitcoin() == true) {
                    setFeeAmount(it, mAssetLiveData.value?.amountWithUnit?.unit)
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
                if (mAssetLiveData.value == null) {
                    setFeeAmount("- -")
                    return@launch
                }
                if (mAmountLiveData.value?.isEmpty() == true
                    || (mAmountLiveData.value?.toDouble() ?: 0.0) == 0.0
                ) {
                    setFeeAmount("0.00", mAssetLiveData.value?.amountWithUnit?.unit)
                    return@launch
                }
                if (mAssetLiveData.value?.isBitcoin() == true) {
                    mTransactionManager.transferAmountIntent(
                        mAmountLiveData.value?.toDouble() ?: 0.0,
                        mFeeProgressLiveData.value ?: DEF_FEE_PROGRESS
                    )
                } else {
                    setFeeAmount("0.00", mAssetLiveData.value?.amountWithUnit?.unit)
                }
            } catch (e: Exception) {
                setFeeAmount("- -")
            }
        }
    }

    /**
     * 检查转账的金额，地址是否合理合法
     */
    fun checkConditions(account: AccountDO) {
        if (BigDecimal(mAssetLiveData.value?.amountWithUnit?.amount ?: "0") <
            BigDecimal(mAmountLiveData.value ?: "0")
        ) {
            throw LackOfBalanceException()
        }

        mTransferManager.checkTransferParam(
            mAmountLiveData.value ?: "0",
            mPayeeAddressLiveData.value ?: "",
            account
        )
    }

    @Throws(AddressFaultException::class, WrongPasswordException::class)
    suspend fun transfer(
        account: AccountDO,
        pwd: ByteArray,
        toSubAddress: String?
    ) {
        val assets = mAssetLiveData.value
            ?: throw RuntimeException(getString(R.string.transfer_tips_token_empty))

        suspendCancellableCoroutine<String> { cancellation ->
            cancellation.invokeOnCancellation {
                throw ForcedStopException()
            }

            if (assets.isBitcoin()) {
                val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
                    .decrypt(pwd, account.privateKey) ?: throw WrongPasswordException()

                mTransferManager.transferBtc(
                    transactionManager = mTransactionManager,
                    payeeAddress = mPayeeAddressLiveData.value ?: "",
                    transferAmount = mAmountLiveData.value?.toDouble() ?: 0.0,
                    privateKey = privateKey,
                    accountDO = account,
                    progress = mFeeProgressLiveData.value ?: DEF_FEE_PROGRESS,
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
                        payeeAddress = mPayeeAddressLiveData.value ?: "",
                        payeeSubAddress = toSubAddress,
                        amountStr = mAmountLiveData.value ?: "",
                        privateKey = privateKey,
                        accountDO = account,
                        progress = mFeeProgressLiveData.value ?: DEF_FEE_PROGRESS,
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