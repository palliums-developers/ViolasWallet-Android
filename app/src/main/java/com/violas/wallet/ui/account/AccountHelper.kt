package com.violas.wallet.ui.account

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.Vm
import com.violas.wallet.getString
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO

/**
 * Created by elephant on 2019-10-25 16:00.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

/**
 * 加载所有指定账户展示类型的账户
 */
fun loadAccounts(@AccountDisplayType displayMode: Int): MutableMap<String, List<AccountVo>> {
    val data = mutableMapOf<String, List<AccountVo>>()

    val currentAccount = AccountManager().currentAccount()
    val accountStorage = DataRepository.getAccountStorage()
    if (displayMode == AccountDisplayType.ALL) {

        val identityAccounts = arrayListOf<AccountVo>()
        val identityAccountLabel = getString(R.string.account_label_identity)
        val identityAccountsTemp = accountStorage.loadAllByWalletType(0)
        identityAccountsTemp.forEach {
            identityAccounts.add(AccountVo(it).apply {
                selected = it.id == currentAccount.id
                setGroupName(identityAccountLabel)
            })
        }
        data[identityAccountLabel] = identityAccounts

        val otherWallets = arrayListOf<AccountVo>()
        val otherAccountLabel = getString(R.string.account_label_other)
        val otherWalletsTemp = accountStorage.loadAllByWalletType(1)
        otherWalletsTemp.forEach {
            otherWallets.add(AccountVo(it).apply {
                selected = it.id == currentAccount.id
                setGroupName(otherAccountLabel)
            })
        }
        data[otherAccountLabel] = otherWallets

    } else {
        var accountLabel = "Violas"
        var coinType = CoinTypes.VToken.coinType()

        when (displayMode) {
            AccountDisplayType.BTC -> {
                accountLabel = "BTC"
                coinType = if (Vm.TestNet) {
                    CoinTypes.BitcoinTest.coinType()
                } else {
                    CoinTypes.Bitcoin.coinType()
                }
            }
            AccountDisplayType.LIBRA -> {
                accountLabel = "Libra"
                coinType = CoinTypes.Libra.coinType()
            }
        }

        val accounts = arrayListOf<AccountVo>()
        val accountsTemp = accountStorage.loadAllByCoinType(coinType)
        accountsTemp.forEach {
            accounts.add(AccountVo(it).apply {
                selected = it.id == currentAccount.id
                setGroupName(accountLabel)
            })
        }
        data[accountLabel] = accounts
    }

    return data
}

/**
 * 生产假账户(code for test)
 */
fun fakeAccounts(@AccountDisplayType displayMode: Int): MutableMap<String, List<AccountVo>> {
    val data = mutableMapOf<String, List<AccountVo>>()

    if (displayMode == AccountDisplayType.ALL) {
        val identityAccountLabel = getString(R.string.account_label_identity)
        val otherAccountLabel = getString(R.string.account_label_other)

        val identityAccounts = arrayListOf(
            AccountVo(
                AccountDO(
                    id = 0,
                    walletNickname = "Violas-Wallet",
                    walletType = 0,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.VToken.coinType()
                ),
                selected = true
            ).apply { setGroupName(identityAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 1,
                    walletNickname = "Libra-Wallet",
                    walletType = 0,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.Libra.coinType()
                )
            ).apply { setGroupName(identityAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 2,
                    walletNickname = "BTC-Wallet",
                    walletType = 0,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.Bitcoin.coinType()
                )
            ).apply { setGroupName(identityAccountLabel) }
        )

        val otherAccounts = arrayListOf(
            AccountVo(
                AccountDO(
                    id = 3,
                    walletNickname = "Violas-Wallet",
                    walletType = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.VToken.coinType()
                )
            ).apply { setGroupName(otherAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 4,
                    walletNickname = "Libra-Wallet",
                    walletType = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.Libra.coinType()
                )
            ).apply { setGroupName(otherAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 5,
                    walletNickname = "BTC-Wallet",
                    walletType = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.Bitcoin.coinType()
                )
            ).apply { setGroupName(otherAccountLabel) }
        )

        data[identityAccountLabel] = identityAccounts
        data[otherAccountLabel] = otherAccounts
    } else {
        var accountLabel = "Violas"
        var coinType = CoinTypes.VToken.coinType()

        when (displayMode) {
            AccountDisplayType.BTC -> {
                accountLabel = "BTC"
                coinType = CoinTypes.Bitcoin.coinType()
            }
            AccountDisplayType.LIBRA -> {
                accountLabel = "Libra"
                coinType = CoinTypes.Libra.coinType()
            }
        }

        val accounts = arrayListOf(AccountVo(
            AccountDO(
                id = 0,
                walletNickname = "$accountLabel-Wallet",
                walletType = 0,
                address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                coinNumber = coinType
            ),
            selected = coinType == CoinTypes.VToken.coinType()
        ).apply { setGroupName(accountLabel) },
            AccountVo(
                AccountDO(
                    id = 1,
                    walletNickname = "$accountLabel-Wallet",
                    walletType = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = coinType
                )
            ).apply { setGroupName(accountLabel) }
        )
        data[accountLabel] = accounts
    }

    return data
}