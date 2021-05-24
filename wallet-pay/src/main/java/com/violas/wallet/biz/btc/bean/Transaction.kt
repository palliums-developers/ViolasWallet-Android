package com.violas.wallet.biz.btc.bean

import java.math.BigInteger

/**
 * Created by elephant on 5/7/21 11:58 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class Transaction(
    val txId: String,   // The hash of the transaction
    val blockHash: String,
    val blockTime: Long,
    val confirmations: Long,
    val txHex: String? = null,
    val blockHeight: Long? = null,
    val lockTime: Long? = null,
    val size: Long? = null,
    val vsize: Long? = null,
    val version: Int? = null,
    val inputs: List<Input>? = null,
    val outputs: List<Output>? = null
) {
    class Input(
        val txId: String?,
        val value: BigInteger,
        val scriptSig: ScriptSig,
        val addresses: List<String>? = null,
        val sequence: Long? = null
    )

    class Output(
        val value: BigInteger,
        val scriptPubKey: ScriptPubKey,
        val addresses: List<String>? = null
    )

    class ScriptSig(
        val hex: String? = null,
        val asm: String? = null,
        val type: String? = null
    )

    class ScriptPubKey(
        val hex: String? = null,
        val asm: String? = null,
        val type: String? = null,
        val reqSigs: Int? = null,
    )
}