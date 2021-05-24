package com.violas.wallet.repository.http.bitcoin.blockCypher

import com.palliums.utils.utcToLocal
import com.violas.wallet.biz.btc.BitcoinBasicService
import com.violas.wallet.biz.btc.BitcoinFeeService
import com.violas.wallet.biz.btc.bean.AccountState
import com.violas.wallet.biz.btc.bean.Fees
import com.violas.wallet.biz.btc.bean.Transaction
import com.violas.wallet.biz.btc.bean.UTXO
import java.math.BigInteger

/**
 * Created by elephant on 5/17/21 10:10 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BitcoinBlockCypherService(
    private val repository: BitcoinBlockCypherRepository,
    private val testnet: Boolean
) : BitcoinBasicService, BitcoinFeeService {

    override suspend fun getAccountState(address: String): AccountState {
        val response = repository.getAccountState(getNetwork(), address)

        return AccountState(
            address = response.address,
            balance = response.balance,
            unconfirmedBalance = response.unconfirmed_balance,
            received = response.total_received,
            sent = response.total_sent,
            txs = response.n_tx,
            unconfirmedTxs = response.unconfirmed_n_tx
        )
    }

    override suspend fun getUTXO(address: String): List<UTXO> {
        val response = repository.getUTXO(getNetwork(), address)

        return response.utxos?.map {
            UTXO(
                txId = it.tx_hash,
                outputNo = it.tx_output_n,
                scriptPubKey = it.script,
                amount = it.value,
                confirmations = it.confirmations
            )
        } ?: listOf()
    }

    override suspend fun getTransaction(txId: String): Transaction {
        val response = repository.getTransaction(getNetwork(), txId)

        return Transaction(
            txId = response.hash,
            blockHash = response.block_hash,
            blockTime = utcToLocal(response.confirmed),
            confirmations = response.confirmations,
            txHex = response.hex,
            blockHeight = response.block_height,
            size = response.size,
            vsize = response.vsize,
            version = response.ver,
            inputs = response.inputs?.map {
                Transaction.Input(
                    txId = it.prev_hash,
                    value = it.output_value ?: BigInteger.ZERO,
                    scriptSig = Transaction.ScriptSig(
                        hex = it.script,
                        type = if (it.script_type.equals("empty", true))
                            null
                        else
                            it.script_type
                    ),
                    addresses = it.addresses,
                    sequence = it.sequence
                )
            },
            outputs = response.outputs?.map {
                Transaction.Output(
                    value = it.value,
                    scriptPubKey = Transaction.ScriptPubKey(hex = it.script, type = it.script_type),
                    addresses = it.addresses
                )
            }
        )
    }

    override suspend fun pushTransaction(tx: String): String {
        return repository.pushTransaction(getNetwork(), tx)
    }

    override suspend fun estimateFee(): Fees {
        val response = repository.getChainState(getNetwork())

        return Fees(
            fastestFee = response.high_fee_per_kb / 1000,
            halfHourFee = response.medium_fee_per_kb / 1000,
            hourFee = response.low_fee_per_kb / 1000
        )
    }

    private fun getNetwork(): String {
        return if (testnet) "test3" else "main"
    }
}