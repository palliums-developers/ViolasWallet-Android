package com.violas.wallet.ui.account

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
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

    val accountStorage = DataRepository.getAccountStorage()
    if (displayMode == AccountDisplayType.ALL) {

        val identityWallets = arrayListOf<AccountVo>()
        val identityWalletLabel = getString(R.string.account_label_identity_wallet)
        val identityWalletsTemp = accountStorage.loadAllByWalletType(0)
        identityWalletsTemp.forEach {
            identityWallets.add(AccountVo(it).apply { setGroupName(identityWalletLabel) })
        }
        data[identityWalletLabel] = identityWallets

        val otherWallets = arrayListOf<AccountVo>()
        val otherWalletLabel = getString(R.string.account_label_other_wallet)
        val otherWalletsTemp = accountStorage.loadAllByWalletType(1)
        otherWalletsTemp.forEach {
            otherWallets.add(AccountVo(it).apply { setGroupName(otherWalletLabel) })
        }
        data[otherWalletLabel] = otherWallets

    } else {
        var walletLabel = "Violas"
        var coinType = CoinTypes.VToken.coinType()

        when (displayMode) {
            AccountDisplayType.BTC -> {
                walletLabel = "BTC"
                coinType = CoinTypes.Bitcoin.coinType()
            }
            AccountDisplayType.LIBRA -> {
                walletLabel = "Libra"
                coinType = CoinTypes.Libra.coinType()
            }
        }

        val wallets = arrayListOf<AccountVo>()
        val walletsTemp = accountStorage.loadAllByCoinType(coinType)
        walletsTemp.forEach {
            wallets.add(AccountVo(it).apply { setGroupName(walletLabel) })
        }
        data[walletLabel] = wallets
    }

    return data
}

/**
 * 生产假账户(code for test)
 */
fun fakeAccounts(): MutableMap<String, List<AccountVo>> {
    val data = mutableMapOf<String, List<AccountVo>>()

    val identityWalletLabel = getString(R.string.account_label_identity_wallet)
    val otherWalletLabel = getString(R.string.account_label_other_wallet)

    val identityWallets = arrayListOf(
        AccountVo(
            AccountDO(
                id = 0,
                walletNickname = "Violas-Wallet",
                walletType = 0,
                address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                coinNumber = CoinTypes.VToken.coinType()
            )
        ).apply { setGroupName(identityWalletLabel) },
        AccountVo(
            AccountDO(
                id = 0,
                walletNickname = "Libra-Wallet",
                walletType = 0,
                address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                coinNumber = CoinTypes.Libra.coinType()
            )
        ).apply { setGroupName(identityWalletLabel) },
        AccountVo(
            AccountDO(
                id = 0,
                walletNickname = "BTC-Wallet",
                walletType = 0,
                address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                coinNumber = CoinTypes.Bitcoin.coinType()
            )
        ).apply { setGroupName(identityWalletLabel) }
    )

    val otherWallets = arrayListOf(
        AccountVo(
            AccountDO(
                id = 0,
                walletNickname = "Violas-Wallet",
                walletType = 1,
                address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                coinNumber = CoinTypes.VToken.coinType()
            )
        ).apply { setGroupName(otherWalletLabel) },
        AccountVo(
            AccountDO(
                id = 0,
                walletNickname = "Libra-Wallet",
                walletType = 1,
                address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                coinNumber = CoinTypes.Libra.coinType()
            )
        ).apply { setGroupName(otherWalletLabel) },
        AccountVo(
            AccountDO(
                id = 0,
                walletNickname = "BTC-Wallet",
                walletType = 1,
                address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                coinNumber = CoinTypes.Bitcoin.coinType()
            )
        ).apply { setGroupName(otherWalletLabel) }
    )

    data[identityWalletLabel] = identityWallets
    data[otherWalletLabel] = otherWallets

    return data
}