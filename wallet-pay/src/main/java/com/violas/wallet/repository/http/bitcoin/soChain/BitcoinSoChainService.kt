package com.violas.wallet.repository.http.bitcoin.soChain

import com.violas.wallet.biz.btc.BitcoinBasicService
import com.violas.wallet.biz.btc.bean.AccountState
import com.violas.wallet.biz.btc.bean.Transaction
import com.violas.wallet.biz.btc.bean.UTXO
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Created by elephant on 5/8/21 3:36 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BitcoinSoChainService(
    private val repository: BitcoinSoChainRepository,
    private val testnet: Boolean
) : BitcoinBasicService {

    override suspend fun getAccountState(address: String): AccountState {
        val dto = repository.getAccountState(getNetwork(), address)

        return AccountState(
            address = dto.address,
            balance = dto.confirmed_balance.toBigDecimalOrNull()
                ?.multiply(BigDecimal(100000000))
                ?.setScale(0, RoundingMode.DOWN)
                ?.toBigInteger() ?: BigInteger.ZERO,
            unconfirmedBalance = dto.unconfirmed_balance.toBigDecimalOrNull()
                ?.multiply(BigDecimal(100000000))
                ?.setScale(0, RoundingMode.DOWN)
                ?.toBigInteger()
        )
    }

    override suspend fun getUTXO(address: String): List<UTXO> {
        val dtos = repository.getUTXO(getNetwork(), address)

        return dtos.map {
            UTXO(
                txId = it.txid,
                outputNo = it.output_no,
                scriptPubKey = it.script_hex,
                amount = it.value.toBigDecimalOrNull()
                    ?.multiply(BigDecimal(100000000))
                    ?.setScale(0, RoundingMode.DOWN)
                    ?.toBigInteger() ?: BigInteger.ZERO,
                confirmations = it.confirmations
            )
        }
    }

    override suspend fun getTransaction(txId: String): Transaction {
        val dto = repository.getTransaction(getNetwork(), txId)

        return Transaction(
            txId = dto.txid,
            blockHash = dto.blockhash,
            blockTime = dto.time,
            confirmations = dto.confirmations,
            txHex = dto.tx_hex,
            lockTime = dto.locktime,
            size = dto.size,
            version = dto.version,
            inputs = dto.inputs?.map {
                Transaction.Input(
                    txId = it.from_output?.txid,
                    value = it.value.toBigDecimalOrNull()
                        ?.multiply(BigDecimal(100000000))
                        ?.setScale(0, RoundingMode.DOWN)
                        ?.toBigInteger() ?: BigInteger.ZERO,
                    scriptSig = Transaction.ScriptSig(hex = it.script, type = it.type),
                    addresses = if (it.address.equals("coinbase", true))
                        null
                    else
                        listOf(it.address)
                )
            },
            outputs = dto.outputs?.map {
                Transaction.Output(
                    value = it.value.toBigDecimalOrNull()
                        ?.multiply(BigDecimal(100000000))
                        ?.setScale(0, RoundingMode.DOWN)
                        ?.toBigInteger() ?: BigInteger.ZERO,
                    scriptPubKey = Transaction.ScriptPubKey(asm = it.script, type = it.type),
                    addresses = listOf(it.address)
                )
            }
        )
    }

    override suspend fun pushTransaction(tx: String): String {
        return repository.pushTransaction(getNetwork(), tx)
    }

    private fun getNetwork(): String {
        return if (testnet) "BTC" else "BTCTEST"
    }
}