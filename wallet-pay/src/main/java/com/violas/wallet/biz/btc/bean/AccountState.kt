package com.violas.wallet.biz.btc.bean

import java.math.BigInteger

/**
 * Created by elephant on 5/7/21 10:44 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class AccountState(
    val address: String,
    val balance: BigInteger,
    val unconfirmedBalance: BigInteger? = null,
    val received: BigInteger? = null,
    val unconfirmedReceived: BigInteger? = null,
    val sent: BigInteger? = null,
    val unconfirmedSent: BigInteger? = null,
    val txs: Int? = null,
    val unconfirmedTxs: Int? = null,
    val unspentTxs: Int? = null,
)