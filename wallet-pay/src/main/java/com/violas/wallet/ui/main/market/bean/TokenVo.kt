package com.violas.wallet.ui.main.market.bean

import com.violas.wallet.repository.database.entity.AccountType
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
    var accountDoId: Long,          // 本地账户id
    val coinNumber: Int,            // 币种类型值
    val displayName: String,        // 币的显示名称
    val logo: String,               // 币的logo
    var displayAmount: BigDecimal,  // 币的金额
    var anchorValue: Double,        // 币的锚定价值
    var selected: Boolean           // 币是否选中
) {
    abstract fun areContentsTheSame(another: ITokenVo): Boolean
}

/**
 * 市场支持的平台币
 */
class PlatformTokenVo(
    accountDoId: Long,
    val accountType: AccountType,   // 账户类型
    var accountAddress: String,     // 账户地址，即钱包地址
    coinNumber: Int,
    displayName: String,
    logo: String,
    displayAmount: BigDecimal = BigDecimal(0),
    anchorValue: Double = 0.00,
    selected: Boolean = false
) : ITokenVo(accountDoId, coinNumber, displayName, logo, displayAmount, anchorValue, selected) {

    override fun areContentsTheSame(another: ITokenVo): Boolean {
        return if (another is PlatformTokenVo)
            accountDoId == another.accountDoId
                    && accountType == another.accountType
                    && accountAddress == another.accountAddress
                    && coinNumber == another.coinNumber
                    && displayName == another.displayName
                    && logo == another.logo
                    && displayAmount == another.displayAmount
                    && anchorValue == another.anchorValue
                    && selected == another.selected
        else
            false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlatformTokenVo
        return accountDoId == other.accountDoId
                && accountType == other.accountType
                && accountAddress == other.accountAddress
                && coinNumber == other.coinNumber
    }

    override fun hashCode(): Int {
        var result = 17
        result = result * 31 + accountDoId.hashCode()
        result = result * 31 + accountType.hashCode()
        result = result * 31 + accountAddress.hashCode()
        result = result * 31 + coinNumber.hashCode()
        result = result * 31 + displayName.hashCode()
        result = result * 31 + logo.hashCode()
        result = result * 31 + displayAmount.hashCode()
        result = result * 31 + anchorValue.hashCode()
        result = result * 31 + selected.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlatformTokenVo(accountDoId=$accountDoId, accountType=$accountType, accountAddress=$accountAddress, coinNumber=$coinNumber, displayName='$displayName', logo='$logo', displayAmount=${displayAmount.toPlainString()})"
    }
}

/**
 * 市场支持的稳定币
 */
class StableTokenVo(
    accountDoId: Long,
    coinNumber: Int,
    val marketIndex: Int,           // 市场支持的币种的索引
    var tokenDoId: Long,            // 本地token的id
    val address: String,            // 稳定币的地址
    val module: String,             // 稳定币的module名
    val name: String,               // 稳定币的名称
    displayName: String,
    logo: String,
    var localEnable: Boolean,       // 本地启用（本地是否显示）
    var chainEnable: Boolean,       // 链上启用（是否已添加到账户）
    displayAmount: BigDecimal = BigDecimal(0),
    anchorValue: Double = 0.00,
    selected: Boolean = false
) : ITokenVo(accountDoId, coinNumber, displayName, logo, displayAmount, anchorValue, selected) {

    override fun areContentsTheSame(another: ITokenVo): Boolean {
        return if (another is StableTokenVo)
            accountDoId == another.accountDoId
                    && coinNumber == another.coinNumber
                    && marketIndex == another.marketIndex
                    && tokenDoId == another.tokenDoId
                    && address == another.address
                    && module == another.module
                    && name == another.name
                    && displayName == another.displayName
                    && logo == another.logo
                    && localEnable == another.localEnable
                    && chainEnable == another.chainEnable
                    && displayAmount == another.displayAmount
                    && anchorValue == another.anchorValue
                    && selected == another.selected
        else
            false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StableTokenVo
        return accountDoId == other.accountDoId
                && coinNumber == other.coinNumber
                && tokenDoId == other.tokenDoId
                && address == other.address
                && module == other.module
                && name == other.name
    }

    override fun hashCode(): Int {
        var result = 17
        result = result * 31 + accountDoId.hashCode()
        result = result * 31 + coinNumber.hashCode()
        result = result * 31 + marketIndex.hashCode()
        result = result * 31 + tokenDoId.hashCode()
        result = result * 31 + address.hashCode()
        result = result * 31 + module.hashCode()
        result = result * 31 + name.hashCode()
        result = result * 31 + displayName.hashCode()
        result = result * 31 + logo.hashCode()
        result = result * 31 + localEnable.hashCode()
        result = result * 31 + chainEnable.hashCode()
        result = result * 31 + displayAmount.hashCode()
        result = result * 31 + anchorValue.hashCode()
        result = result * 31 + selected.hashCode()
        return result
    }

    override fun toString(): String {
        return "StableTokenVo(accountDoId=$accountDoId, coinNumber=$coinNumber, marketIndex=$marketIndex, tokenDoId=$tokenDoId, address='$address', module='$module', name='$name', displayName='$displayName', logo='$logo', localEnable=$localEnable, chainEnable=$chainEnable, displayAmount=${displayAmount.toPlainString()})"
    }
}