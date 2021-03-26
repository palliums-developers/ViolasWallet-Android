package com.violas.wallet.viewModel.bean

import com.violas.wallet.biz.bean.DiemCurrency
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.database.entity.AccountType

// todo 暂时使用 Serializable
sealed class AssetVo {
    val amountWithUnit by lazy { AmountWithUnit("0.00", "") }
    val fiatAmountWithUnit by lazy { FiatAmountWithUnit("0.00", "$", "") }
    private var assetsName: String = ""

    fun getAssetsName() = assetsName
    fun setAssetsName(assetsName: String) {
        this.assetsName = assetsName
    }

    abstract fun getId(): Long
    abstract fun getAccountId(): Long
    abstract fun getAmount(): Long
    abstract fun setAmount(amount: Long)
    abstract fun getLogoUrl(): String
    abstract fun getCoinNumber(): Int

    fun isBitcoin(): Boolean {
        return getCoinNumber() == getBitcoinCoinType().coinNumber()
    }

    fun isDiem(): Boolean {
        return getCoinNumber() == getDiemCoinType().coinNumber()
    }

    fun isViolas(): Boolean {
        return getCoinNumber() == getViolasCoinType().coinNumber()
    }
}

data class FiatAmountWithUnit(
    var amount: String,
    var symbol: String,
    var unit: String,
    var rate: String = "0"
)

data class AmountWithUnit(
    var amount: String,
    var unit: String
)

open class CoinAssetVo(
    private val id: Long,
    private val coinNumber: Int,
    val address: String,
    private var amount: Long = 0,
    private val logo: String = "",
    val accountType: AccountType = AccountType.Normal
) : AssetVo() {
    override fun getId() = id
    override fun getAccountId() = id

    override fun getAmount() = amount
    override fun setAmount(amount: Long) {
        this.amount = amount
    }

    override fun getCoinNumber(): Int = coinNumber
    override fun getLogoUrl() = logo

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CoinAssetVo
        return id == other.getId()
                && coinNumber == other.getCoinNumber()
    }
}

class DiemCoinAssetVo(
    id: Long,
    coinNumber: Int,
    address: String,
    amount: Long = 0,
    logo: String = "",
    var authKey: String? = "",
    var authKeyPrefix: String = "",
    var delegatedKeyRotationCapability: Boolean = false,
    var delegatedWithdrawalCapability: Boolean = false
) : CoinAssetVo(
    id,
    coinNumber,
    address,
    amount,
    logo,
    AccountType.NoDollars
)

data class DiemCurrencyAssetVo(
    private val id: Long,
    private val accountId: Long,
    private val coinNumber: Int,
    val currency: DiemCurrency,
    val enable: Boolean = false,
    private var amount: Long = 0,
    private var logo: String = ""
) : AssetVo() {
    override fun getId() = id
    override fun getAccountId() = accountId

    override fun getAmount() = amount
    override fun setAmount(amount: Long) {
        this.amount = amount
    }

    override fun getCoinNumber(): Int = coinNumber
    override fun getLogoUrl() = logo

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiemCurrencyAssetVo
        return id == other.getId()
                && accountId == other.accountId
                && coinNumber == other.getCoinNumber()
                && currency == other.currency

    }
}