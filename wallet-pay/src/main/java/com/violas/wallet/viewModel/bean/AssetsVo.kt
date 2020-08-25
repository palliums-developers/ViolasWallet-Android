package com.violas.wallet.viewModel.bean

import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bip44.CoinType
import com.violas.wallet.repository.database.entity.AccountType

// todo 暂时使用 Serializable
abstract class AssetsVo {
    val amountWithUnit: AmountWithUnit = AmountWithUnit("0.00", "")
    val fiatAmountWithUnit: FiatAmountWithUnit = FiatAmountWithUnit("0.00", "$", "")
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
        return getCoinNumber() == CoinTypes.Bitcoin.coinType() || getCoinNumber() == CoinTypes.BitcoinTest.coinType()
    }

    fun isLibra(): Boolean {
        return getCoinNumber() == CoinTypes.Libra.coinType()
    }

    fun isViolas(): Boolean {
        return getCoinNumber() == CoinTypes.Violas.coinType()
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

open class AssetsCoinVo(
    private val id: Long,
    var publicKey: String = "",
    private val coinNumber: Int,
    var address: String = "",
    private var amount: Long = 0,
    var accountType: AccountType = AccountType.Normal,
    private val logo: String
) : AssetsVo() {
    override fun getId() = id
    override fun getAccountId() = id

    override fun getAmount() = amount
    override fun setAmount(amount: Long) {
        this.amount = amount
    }

    override fun getLogoUrl() = logo
    override fun getCoinNumber(): Int = coinNumber
}

class AssetsLibraCoinVo(
    id: Long,
    publicKey: String = "",
    var authKey: String = "",
    var authKeyPrefix: String = "",
    coinNumber: Int = 0,
    address: String = "",
    amount: Long = 0,
    logo: String = "",
    var delegatedKeyRotationCapability: Boolean = false,
    var delegatedWithdrawalCapability: Boolean = false
) : AssetsCoinVo(
    id,
    publicKey,
    coinNumber,
    address,
    amount,
    AccountType.NoDollars,
    logo
)

data class AssetsTokenVo(
    private val id: Long,
    private val accountId: Long,
    private val coinNumber: Int,
    val address: String = "00000000000000000000000000000000",
    val module: String = "LBR",
    val name: String = "T",
    val enable: Boolean = false,
    private var amount: Long = 0,
    private var logo: String = ""
) : AssetsVo() {
    override fun getId() = id
    override fun getAccountId() = accountId
    override fun getAmount() = amount
    override fun setAmount(amount: Long) {
        this.amount = amount
    }

    override fun getCoinNumber(): Int = coinNumber
    override fun getLogoUrl() = logo
}

data class HiddenTokenVo(
    private val id: Long,
    private val accountId: Long,
    private val coinNumber: Int,
    val address: String = "00000000000000000000000000000000",
    val module: String = "LBR",
    val name: String = "T",
    val enable: Boolean = false,
    private var amount: Long = 0
) : AssetsVo() {
    override fun getId() = id
    override fun getAccountId() = accountId
    override fun getAmount() = amount
    override fun setAmount(amount: Long) {
        this.amount = amount
    }

    override fun getCoinNumber(): Int = coinNumber
    override fun getLogoUrl() = ""
}