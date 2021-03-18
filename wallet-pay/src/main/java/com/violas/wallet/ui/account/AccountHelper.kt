package com.violas.wallet.ui.account

import com.palliums.utils.getString
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.dao.AccountDao
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
fun loadAccounts(
    @AccountType accountType: Int,
    accountDao: AccountDao = DataRepository.getAccountStorage(),
    differentiateIdentityIfNotAllAccountType: Boolean = false // 不是AccountType.ALL时，区分身份钱包和创建导入钱包
): MutableMap<String, List<AccountVo>> {
    val data = mutableMapOf<String, List<AccountVo>>()

    val currentAccount = AccountManager.currentAccount()
    if (accountType == AccountType.ALL) {

        val identityAccountLabel = getString(R.string.wallet_account_label_identity)
        val identityAccounts = accountDao.loadAllByWalletType()
            .map {
                AccountVo(it).apply {
                    selected = it.id == currentAccount.id
                    setGroupName(identityAccountLabel)
                }
            }
        data[identityAccountLabel] = identityAccounts

        val otherAccountLabel = getString(R.string.wallet_account_label_other)
        val otherAccounts = accountDao.loadAllByWalletType()
            .map {
                AccountVo(it).apply {
                    selected = it.id == currentAccount.id
                    setGroupName(otherAccountLabel)
                }
            }
        data[otherAccountLabel] = otherAccounts

    } else {
        val coinTypes = transformAccountType(accountType)
        if (differentiateIdentityIfNotAllAccountType) {
            val identityAccountLabel = getString(R.string.wallet_account_label_identity)
            val identityAccounts =
                accountDao.findAllByCoinType(coinTypes.coinNumber())
                    ?.map {
                        AccountVo(it).apply {
                            selected = it.id == currentAccount.id
                            setGroupName(identityAccountLabel)
                        }
                    }
            identityAccounts?.let {
                data[identityAccountLabel] = identityAccounts
            }

            val otherAccountLabel = getString(R.string.wallet_account_label_other)
            val otherAccounts =
                accountDao.findAllByCoinType(coinTypes.coinNumber())
                    ?.map {
                        AccountVo(it).apply {
                            selected = it.id == currentAccount.id
                            setGroupName(otherAccountLabel)
                        }
                    }
            otherAccounts?.let {
                data[otherAccountLabel] = otherAccounts
            }
        } else {
            val accounts = accountDao.loadAllByCoinType(coinTypes.coinNumber())
                .map {
                    AccountVo(it).apply {
                        selected = it.id == currentAccount.id
                        setGroupName(coinTypes.coinName())
                    }
                }
            data[coinTypes.coinName()] = accounts
        }
    }

    return data
}

/**
 * 生产假账户(code for test)
 */
fun fakeAccounts(@AccountType accountType: Int): MutableMap<String, List<AccountVo>> {
    val data = mutableMapOf<String, List<AccountVo>>()

    if (accountType == AccountType.ALL) {
        val identityAccountLabel = getString(R.string.wallet_account_label_identity)
        val otherAccountLabel = getString(R.string.wallet_account_label_other)

        val identityAccounts = arrayListOf(
            AccountVo(
                AccountDO(
                    id = 0,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = getViolasCoinType().coinNumber()
                ),
                selected = true
            ).apply { setGroupName(identityAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 1,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = getDiemCoinType().coinNumber()
                )
            ).apply { setGroupName(identityAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 2,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = getBitcoinCoinType().coinNumber()
                )
            ).apply { setGroupName(identityAccountLabel) }
        )

        val otherAccounts = arrayListOf(
            AccountVo(
                AccountDO(
                    id = 3,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = getViolasCoinType().coinNumber()
                )
            ).apply { setGroupName(otherAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 4,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = getDiemCoinType().coinNumber()
                )
            ).apply { setGroupName(otherAccountLabel) },
            AccountVo(
                AccountDO(
                    id = 5,
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = getBitcoinCoinType().coinNumber()
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
//                    walletNickname = "$${coinTypes.coinName()}-Wallet",
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = coinTypes.coinNumber()
                ),
                selected = coinTypes.coinNumber() == getViolasCoinType().coinNumber()
            ).apply { setGroupName(coinTypes.coinName()) },
            AccountVo(
                AccountDO(
                    id = 1,
//                    walletNickname = "${coinTypes.coinName()}-Wallet 2",
                    address = "mkYUsJ8N1AidN…QUaoyL2Mu8L",
                    coinNumber = coinTypes.coinNumber()
                )
            ).apply { setGroupName(coinTypes.coinName()) }
        )
        data[coinTypes.coinName()] = accounts
    }

    return data
}


@AccountType
fun transformCoinTypes(coinType: CoinType): Int {
    return when (coinType) {
        getViolasCoinType() -> {
            AccountType.VIOLAS
        }

        getDiemCoinType() -> {
            AccountType.LIBRA
        }

        else -> {
            AccountType.BTC
        }
    }
}

fun transformAccountType(@AccountType accountType: Int): CoinType {
    return when (accountType) {
        AccountType.VIOLAS -> {
            getViolasCoinType()
        }

        AccountType.LIBRA -> {
            getDiemCoinType()
        }

        else -> {
            getBitcoinCoinType()
        }
    }
}