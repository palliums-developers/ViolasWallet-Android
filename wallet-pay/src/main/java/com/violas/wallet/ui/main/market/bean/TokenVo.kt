package com.violas.wallet.ui.main.market.bean

import java.math.BigDecimal


/**
 * Created by elephant on 2020/7/6 19:03.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

/**
 * 市场支持的币
 */
abstract class ITokenVo(
    val coinNumber: Int,            // 币种类型值
    val displayName: String,        // 币的显示名称
    val logo: String,               // 币的logo
    var displayAmount: BigDecimal,  // 币的金额
    var anchorValue: Double         // 币的锚定价值
) {
    abstract fun areContentsTheSame(another: ITokenVo): Boolean
}

/**
 * 市场支持的平台币
 */
class PlatformTokenVo(
    coinNumber: Int,
    displayName: String,
    logo: String,
    displayAmount: BigDecimal = BigDecimal.ZERO,
    anchorValue: Double = 0.00
) : ITokenVo(coinNumber, displayName, logo, displayAmount, anchorValue) {

    override fun areContentsTheSame(another: ITokenVo): Boolean {
        return if (another is PlatformTokenVo)
            coinNumber == another.coinNumber
                    && displayName == another.displayName
                    && logo == another.logo
                    && displayAmount == another.displayAmount
                    && anchorValue == another.anchorValue
        else
            false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlatformTokenVo
        return coinNumber == other.coinNumber
                && displayName == other.displayName
    }

    override fun hashCode(): Int {
        var result = 17
        result = result * 31 + coinNumber.hashCode()
        result = result * 31 + displayName.hashCode()
        result = result * 31 + logo.hashCode()
        result = result * 31 + displayAmount.hashCode()
        result = result * 31 + anchorValue.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlatformTokenVo(coinNumber=$coinNumber, displayName='$displayName', logo='$logo', displayAmount=${displayAmount.toPlainString()}, anchorValue=$anchorValue)"
    }
}

/**
 * 市场支持的稳定币
 */
class StableTokenVo(
    val name: String,               // 稳定币的名称
    val module: String,             // 稳定币的module名
    val address: String,            // 稳定币的地址
    val marketIndex: Int,           // 市场支持的币种的索引
    coinNumber: Int,
    displayName: String,
    logo: String,
    displayAmount: BigDecimal = BigDecimal.ZERO,
    anchorValue: Double = 0.00
) : ITokenVo(coinNumber, displayName, logo, displayAmount, anchorValue) {

    override fun areContentsTheSame(another: ITokenVo): Boolean {
        return if (another is StableTokenVo)
            name == another.name
                    && module == another.module
                    && address == another.address
                    && marketIndex == another.marketIndex
                    && coinNumber == another.coinNumber
                    && displayName == another.displayName
                    && logo == another.logo
                    && displayAmount == another.displayAmount
                    && anchorValue == another.anchorValue
        else
            false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StableTokenVo
        return name == other.name
                && module == other.module
                && address == other.address
                && coinNumber == other.coinNumber
    }

    override fun hashCode(): Int {
        var result = 17
        result = result * 31 + name.hashCode()
        result = result * 31 + module.hashCode()
        result = result * 31 + address.hashCode()
        result = result * 31 + marketIndex.hashCode()
        result = result * 31 + coinNumber.hashCode()
        result = result * 31 + displayName.hashCode()
        result = result * 31 + logo.hashCode()
        result = result * 31 + displayAmount.hashCode()
        result = result * 31 + anchorValue.hashCode()
        return result
    }

    override fun toString(): String {
        return "StableTokenVo(name='$name', module='$module', address='$address', marketIndex=$marketIndex, coinNumber=$coinNumber, displayName='$displayName', logo='$logo', displayAmount=${displayAmount.toPlainString()}, anchorValue=$anchorValue)"
    }
}