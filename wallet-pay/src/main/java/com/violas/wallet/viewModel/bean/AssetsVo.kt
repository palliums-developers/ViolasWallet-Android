package com.violas.wallet.viewModel.bean

abstract class AssetsVo {
    private var amountWithUnit: AmountWithUnit = AmountWithUnit("0.00", "")
    private var fiatAmountWithUnit: FiatAmountWithUnit = FiatAmountWithUnit("0.00", "", "")

    abstract fun getId(): Long
    abstract fun getAccountId(): Long
    abstract fun getAssetsName(): String
    abstract fun getAmount(): Long
    abstract fun setAmount(amount: Long)
    abstract fun getLogoUrl(): String

    fun getAmountWithUnit() = amountWithUnit
    fun setAmountWithUnit(unit: AmountWithUnit) {
        amountWithUnit = unit
    }

    fun getFiatAmountWithUnit() = fiatAmountWithUnit
    fun setFiatAmountWithUnit(unit: FiatAmountWithUnit) {
        fiatAmountWithUnit = unit
    }
}

data class FiatAmountWithUnit(
    val amount: String,
    val symbol: String,
    val unit: String
)

data class AmountWithUnit(
    val amount: String,
    val unit: String
)

data class AssetsCoinVo(
    private val id: Long,
    var publicKey: String = "",
    var authKeyPrefix: String = "",
    var coinNumber: Int = 0,
    var address: String = "",
    private var amount: Long = 0,
    private val logo: String
) : AssetsVo() {
    override fun getId() = id
    override fun getAccountId() = id
    override fun getAssetsName() = "BTC"

    override fun getAmount() = amount
    override fun setAmount(amount: Long) {
        this.amount = amount
    }

    override fun getLogoUrl() = logo
}

data class AssetsTokenVo(
    private val id: Long,
    private val account_id: Long,
    val address: String = "00000000000000000000000000000000",
    val module: String = "LBR",
    val name: String = "T",
    val enable: Boolean = false,
    private var amount: Long = 0,
    private var logo: String = ""
) : AssetsVo() {
    override fun getId() = id
    override fun getAccountId() = account_id
    override fun getAmount() = amount
    override fun setAmount(amount: Long) {
        this.amount = amount
    }

    override fun getLogoUrl() = logo
    override fun getAssetsName() = module
}