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
 * 加载所有指定账户类型的账户
 */
fun loadAccounts(@AccountType accountType: Int): MutableMap<String, List<AccountVo>> {
    val data = mutableMapOf<String, List<AccountVo>>()

    val currentAccount = AccountManager().currentAccount()
    val accountStorage = DataRepository.getAccountStorage()
    if (accountType == AccountType.ALL) {

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
        val coinTypes = transformAccountType(accountType)
        val accounts = arrayListOf<AccountVo>()
        val accountsTemp = accountStorage.loadAllByCoinType(coinTypes.coinType())
        accountsTemp.forEach {
            accounts.add(AccountVo(it).apply {
                selected = it.id == currentAccount.id
                setGroupName(coinTypes.coinName())
            })
        }
        data[coinTypes.coinName()] = accounts
    }

    return data
}

/**
 * 生产假账户(code for test)
 */
fun fakeAccounts(@AccountType accountType: Int): MutableMap<String, List<AccountVo>> {
    val data = mutableMapOf<String, List<AccountVo>>()

    if (accountType == AccountType.ALL) {
        val identityAccountLabel = getString(R.string.account_label_identity)
        val otherAccountLabel = getString(R.string.account_label_other)

        val identityAccounts = arrayListOf(
            AccountVo(
                AccountDO(
                    id = 0,
                    walletNickname = "${CoinTypes.VToken.coinName()}-Wallet",
                    walletType = 0,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.VToken.coinType()
                ),
                selected = true
            ).apply { setGroupName(identityAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 1,
                    walletNickname = "${CoinTypes.Libra.coinName()}-Wallet",
                    walletType = 0,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.Libra.coinType()
                )
            ).apply { setGroupName(identityAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 2,
                    walletNickname = "${CoinTypes.Bitcoin.coinName()}-Wallet",
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
                    walletNickname = "${CoinTypes.VToken.coinName()}-Wallet 2",
                    walletType = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.VToken.coinType()
                )
            ).apply { setGroupName(otherAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 4,
                    walletNickname = "${CoinTypes.Libra.coinName()}-Wallet 2",
                    walletType = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.Libra.coinType()
                )
            ).apply { setGroupName(otherAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 5,
                    walletNickname = "${CoinTypes.Bitcoin.coinName()}-Wallet 2",
                    walletType = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = CoinTypes.Bitcoin.coinType()
                )
            ).apply { setGroupName(otherAccountLabel) }
        )

        data[identityAccountLabel] = identityAccounts
        data[otherAccountLabel] = otherAccounts
    } else {
        val coinTypes = transformAccountType(accountType)
        val accounts = arrayListOf(
            AccountVo(
                AccountDO(
                    id = 0,
                    walletNickname = "$${coinTypes.coinName()}-Wallet",
                    walletType = 0,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = coinTypes.coinType()
                ),
                selected = coinTypes.coinType() == CoinTypes.VToken.coinType()
            ).apply { setGroupName(coinTypes.coinName()) },
            AccountVo(
                AccountDO(
                    id = 1,
                    walletNickname = "${coinTypes.coinName()}-Wallet 2",
                    walletType = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = coinTypes.coinType()
                )
            ).apply { setGroupName(coinTypes.coinName()) }
        )
        data[coinTypes.coinName()] = accounts
    }

    return data
}


@AccountType
fun transformCoinTypes(coinTypes: CoinTypes): Int {
    return when (coinTypes) {
        CoinTypes.VToken -> {
            AccountType.VIOLAS
        }

        CoinTypes.Libra -> {
            AccountType.LIBRA
        }

        else -> {
            AccountType.BTC
        }
    }
}

fun transformAccountType(@AccountType accountType: Int): CoinTypes {
    return when (accountType) {
        AccountType.VIOLAS -> {
            CoinTypes.VToken
        }

        AccountType.LIBRA -> {
            CoinTypes.Libra
        }

        else -> {
            if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin
        }
    }
}