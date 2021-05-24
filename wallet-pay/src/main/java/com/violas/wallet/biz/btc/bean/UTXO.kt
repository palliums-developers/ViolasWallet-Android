package com.violas.wallet.biz.btc.bean

import androidx.annotation.IntDef
import com.quincysx.crypto.bitcoin.script.Script
import com.quincysx.crypto.utils.HexUtils
import java.math.BigInteger

/**
 * Created by elephant on 5/6/21 6:18 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class UTXO(
    val txId: String,
    val outputNo: Int,
    val scriptPubKey: String,
    val amount: BigInteger,
    val confirmations: Long
) {

    companion object {
        /**
         * P2PKH
         */
        const val NONE = -1

        /**
         * P2PKH
         */
        const val P2PKH = 0

        /**
         * P2SH
         */
        const val P2SH = 1

        /**
         * 定期合同
         */
        const val DEPOSIT = 2

        /**
         * 活期合同
         */
        const val DEMAND_DEPOSIT = 3
    }

    @IntDef(NONE, P2PKH, P2SH, DEPOSIT, DEMAND_DEPOSIT)
    annotation class UTXOType

    @UTXOType
    fun getUTXOType(): Int {
        val script = HexUtils.fromHex(scriptPubKey)
        return if (script.size >= 2
            && script[0] == Script.OP_DUP
            && script[1] == Script.OP_HASH160
        ) {
            P2PKH
        } else if (script.isNotEmpty()
            && script[0] == Script.OP_HASH160
        ) {
            P2SH
        } else {
            NONE
        }
    }
}