package com.violas.wallet.biz.transaction

import com.violas.wallet.biz.ReceiverAccountCurrencyNotAddException
import com.violas.wallet.biz.ReceiverAccountNotActivationException
import com.violas.wallet.biz.SenderAccountNoControlException
import com.violas.wallet.biz.SenderAccountNotActivationException
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.common.getViolasCoinType
import org.palliums.violascore.common.*
import org.palliums.violascore.http.AccountStateDTO
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 3/24/21 10:58 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ViolasTxnManager {

    /**
     * 获取 Sender AccountState
     */
    @Throws(
        SenderAccountNotActivationException::class,
        SenderAccountNoControlException::class
    )
    inline fun getSenderAccountState(
        account: Account,
        address: String? = null,
        getAccountState: (address: String) -> AccountStateDTO?
    ): AccountStateDTO {
        val accountState = getAccountState.invoke(address ?: account.getAddress().toHex())
            ?: throw SenderAccountNotActivationException()

        if (accountState.authenticationKey != account.getAuthenticationKey().toHex())
            throw SenderAccountNoControlException()

        return accountState
    }

    /**
     * 获取 Receiver AccountState
     */
    @Throws(
        ReceiverAccountNotActivationException::class,
        ReceiverAccountCurrencyNotAddException::class
    )
    inline fun getReceiverAccountState(
        address: String,
        appToken: DiemAppToken,
        getAccountState: (address: String) -> AccountStateDTO?
    ): AccountStateDTO {
        val accountState = getAccountState.invoke(address)
            ?: throw ReceiverAccountNotActivationException()

        var isAdded = false
        accountState.balances?.let {
            for (balance in it) {
                if (balance.currency.equals(appToken.currency.module, true)) {
                    isAdded = true
                    break
                }
            }
        }
        if (!isAdded)
            throw ReceiverAccountCurrencyNotAddException(
                getViolasCoinType(),
                address,
                appToken
            )

        return accountState
    }


    /**
     * 计算gas信息
     * @param accountState      账户信息
     * @param txnCurrencies     交易的币种信息
     * @param gasCurrencyCode
     * @param maxGasAmount
     * @param gasUnitPrice
     */
    fun calculateGasInfo(
        accountState: AccountStateDTO,
        txnCurrencies: List<Pair<String, Long>>?,
        gasCurrencyCode: String = CURRENCY_DEFAULT_CODE,
        maxGasAmount: Long = MAX_GAS_AMOUNT_DEFAULT,
        gasUnitPrice: Long = GAS_UNIT_PRICE_DEFAULT
    ): ViolasGasInfo {
        // 计算作为gas currency的可用金额，要减去交易出去的金额
        var gasCurrencyAvailableAmount = 0L
        accountState.balances?.let {
            for (balance in it) {
                if (balance.currency.equals(gasCurrencyCode, true)) {
                    gasCurrencyAvailableAmount = balance.amount
                    break
                }
            }
        }
        txnCurrencies?.let {
            for (txnCurrency in it) {
                if (txnCurrency.first.equals(gasCurrencyCode, true)) {
                    gasCurrencyAvailableAmount -= txnCurrency.second
                    break
                }
            }
        }

        // 计算实际的maxGasAmount和gasUnitPrice
        var actualMaxGasAmount = maxGasAmount.coerceIn(MAX_GAS_AMOUNT_MIN, MAX_GAS_AMOUNT_MAX)
        var actualGasUnitPrice = gasUnitPrice.coerceIn(GAS_UNIT_PRICE_MIN, GAS_UNIT_PRICE_MAX)
        if (gasCurrencyAvailableAmount <= 0) {
            // gas currency没有可用金额时，gasUnitPrice置为0
            actualGasUnitPrice = GAS_UNIT_PRICE_MIN
        } else if (actualMaxGasAmount * actualGasUnitPrice > gasCurrencyAvailableAmount) {
            when {
                gasCurrencyAvailableAmount < MAX_GAS_AMOUNT_MIN -> {
                    // gas currency的可用金额不足，且小于系统规定maxGasAmount的最小值时，gasUnitPrice置为0
                    actualGasUnitPrice = GAS_UNIT_PRICE_MIN
                }
                gasCurrencyAvailableAmount > MAX_GAS_AMOUNT_MAX -> {
                    // gas currency的可用金额不足，且大于系统规定maxGasAmount的最大值时，gasUnitPrice置为1
                    actualGasUnitPrice = GAS_UNIT_PRICE_DEFAULT
                }
                else -> {
                    // gas currency的可用金额不足，且在系统规定maxGasAmount的取值区间时，gasUnitPrice置为1，
                    // maxGasAmount在系统规定maxGasAmount的最小值和gas currency的可用金额区间取值
                    actualGasUnitPrice = GAS_UNIT_PRICE_DEFAULT
                    actualMaxGasAmount =
                        actualMaxGasAmount.coerceIn(MAX_GAS_AMOUNT_MIN, gasCurrencyAvailableAmount)
                }
            }
        }

        return ViolasGasInfo(gasCurrencyCode, actualMaxGasAmount, actualGasUnitPrice)
    }
}