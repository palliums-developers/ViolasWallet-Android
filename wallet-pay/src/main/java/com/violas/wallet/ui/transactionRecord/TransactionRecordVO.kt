package com.violas.wallet.ui.transactionRecord

import android.os.Parcelable
import com.quincysx.crypto.CoinTypes
import kotlinx.android.parcel.Parcelize

/**
 * Created by elephant on 2019-11-07 11:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易记录的ViewObject
 */
@Parcelize
data class TransactionRecordVO(
    val id: Int,
    val coinType: CoinTypes,
    @TransactionType
    val transactionType: Int,
    @TransactionState
    val transactionState: Int,
    val time: Long,
    val fromAddress: String,
    val toAddress: String?,
    val amount: String,
    val tokenId: String?,
    val tokenDisplayName: String?,
    val gas: String,
    val gasTokenId: String?,
    val gasTokenDisplayName: String?,
    val transactionId: String,     // libra/violas时为version，bitcoin时为txid
    val url: String?
) : Parcelable