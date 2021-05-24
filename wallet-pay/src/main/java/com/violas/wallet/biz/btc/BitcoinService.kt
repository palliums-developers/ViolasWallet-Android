package com.violas.wallet.biz.btc

import com.violas.wallet.biz.btc.bean.AccountState
import com.violas.wallet.biz.btc.bean.Fees
import com.violas.wallet.biz.btc.bean.Transaction
import com.violas.wallet.biz.btc.bean.UTXO

/**
 * Created by elephant on 5/6/21 4:39 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface BitcoinBasicService {

    suspend fun getAccountState(address: String): AccountState

    suspend fun getUTXO(address: String): List<UTXO>

    suspend fun getTransaction(txId: String): Transaction

    suspend fun pushTransaction(tx: String): String
}

interface BitcoinFeeService {

    suspend fun estimateFee(): Fees
}