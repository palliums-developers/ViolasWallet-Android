package com.violas.wallet.repository.http.bitcoinChainApi.request

import com.google.gson.annotations.SerializedName
import com.violas.wallet.repository.http.bitcoinChainApi.bean.TransactionBean
import com.violas.wallet.repository.http.bitcoinChainApi.bean.UTXO
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import retrofit2.http.GET
import retrofit2.http.Path
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

class TrezorRequest(private val testNet: Boolean) :
    BaseRequest<TrezorRequest.Api?>(),
    BaseBitcoinChainRequest {
    interface Api {
        @GET("v2/address/{address}")
        fun getBalance(@Path("address") address: String): Observable<BalanceDTO>

        @GET("v2/utxo/{address}")
        fun getUTXO(@Path("address") address: String): Observable<UTXODTO>


        @GET("v2/tx/{txhash}")
        fun getTx(@Path("txhash") txhash: String): Observable<TxRequestDTO>

//        @POST("v2/sendtx")
//        fun pushTx(@Body tx: RequestBody): Observable<PushTxDTO>

        @GET("v2/sendtx/{txhash}")
        fun pushTx(@Path("txhash") tx: String): Observable<PushTxDTO>
    }

    override fun getUtxo(address: String): Observable<List<UTXO>> {
        return request?.getUTXO(address)?.map {
            parse(it, address)
        } ?: throw RuntimeException()
    }

    override fun getBalance(address: String): Observable<BigDecimal> {
        return request?.getBalance(address)?.map {
            BigDecimal(it.balance)
        } ?: throw RuntimeException()
    }

    override fun pushTx(tx: String): Observable<String> {
//        val requestBody = tx.toRequestBody("application/json".toMediaTypeOrNull())
//
//        return request?.pushTx(requestBody)?.map {
//            it.result
//        } ?: throw RuntimeException()
        return request?.pushTx(tx)?.map {
            it.result
        } ?: throw RuntimeException()
    }

    override fun getTranscation(TXHash: String): Observable<TransactionBean> {
        return request?.getTx(TXHash)?.map {
            parse(it)
        } ?: throw RuntimeException()
    }

    override fun requestUrl(): String {
        return if (testNet) {
            "https://tbtc1.trezor.io/api/"
        } else {
            "https://btc1.trezor.io/api/"
        }
    }

    override fun requestApi(): Class<Api?>? {
        return Api::class.java as Class<Api?>?
    }

    data class BalanceDTO(
        @SerializedName("address")
        val address: String,
        @SerializedName("balance")
        val balance: String,
        @SerializedName("itemsOnPage")
        val itemsOnPage: Int,
        @SerializedName("page")
        val page: Int,
        @SerializedName("totalPages")
        val totalPages: Int,
        @SerializedName("totalReceived")
        val totalReceived: String,
        @SerializedName("totalSent")
        val totalSent: String,
        @SerializedName("txids")
        val txids: List<String>,
        @SerializedName("txs")
        val txs: Long,
        @SerializedName("unconfirmedBalance")
        val unconfirmedBalance: String,
        @SerializedName("unconfirmedTxs")
        val unconfirmedTxs: Long
    )

    class UTXODTO : ArrayList<UTXOItemDTO>()

    data class UTXOItemDTO(
        @SerializedName("coinbase")
        val coinbase: Boolean,
        @SerializedName("confirmations")
        val confirmations: Long,
        @SerializedName("height")
        val height: Long,
        @SerializedName("lockTime")
        val lockTime: Long,
        @SerializedName("txid")
        val txid: String,
        @SerializedName("value")
        val value: String,
        @SerializedName("vout")
        val vout: Int
    )

    data class PushTxDTO(
        @SerializedName("error")
        val error: Error,
        @SerializedName("result")
        val result: String
    )

    data class Error(
        @SerializedName("message")
        val message: String
    )

    data class TxRequestDTO(
        @SerializedName("blockHash")
        val blockHash: String,
        @SerializedName("blockHeight")
        val blockHeight: Long,
        @SerializedName("blockTime")
        val blockTime: Long,
        @SerializedName("confirmations")
        val confirmations: Long,
        @SerializedName("fees")
        val fees: String,
        @SerializedName("hex")
        val hex: String,
        @SerializedName("txid")
        val txid: String,
        @SerializedName("value")
        val value: String,
        @SerializedName("valueIn")
        val valueIn: String,
        @SerializedName("version")
        val version: Int,
        @SerializedName("vin")
        val vin: List<VinDTO>,
        @SerializedName("vout")
        val vout: List<VoutDTO>
    )

    data class VinDTO(
        @SerializedName("addresses")
        val addresses: List<String>,
        @SerializedName("hex")
        val hex: String,
        @SerializedName("isAddress")
        val isAddress: Boolean,
        @SerializedName("n")
        val n: Int,
        @SerializedName("sequence")
        val sequence: Long,
        @SerializedName("txid")
        val txid: String,
        @SerializedName("value")
        val value: String,
        @SerializedName("vout")
        val vout: Int
    )

    data class VoutDTO(
        @SerializedName("addresses")
        val addresses: List<String>,
        @SerializedName("hex")
        val hex: String,
        @SerializedName("isAddress")
        val isAddress: Boolean,
        @SerializedName("n")
        val n: Int,
        @SerializedName("value")
        val value: String
    )

    private fun parse(it: UTXODTO, address: String): List<UTXO> {
        val utxos = ArrayList<UTXO>(it.size)
        it.forEach {
            utxos.add(parse(it, address))
        }
        return utxos
    }

    private fun parse(it: UTXOItemDTO, address: String): UTXO {
        val countDownLatch =
            CountDownLatch(1)
        val transaction =
            arrayOfNulls<TransactionBean>(1)

        getTranscation(it.txid)
            .subscribe(object :
                Observer<TransactionBean?> {
                override fun onSubscribe(d: Disposable) {}

                override fun onError(e: Throwable) {
                    countDownLatch.countDown()
                }

                override fun onComplete() {}
                override fun onNext(t: TransactionBean) {
                    transaction[0] = t
                    countDownLatch.countDown()
                }
            })

        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        if (transaction[0] == null) {
            throw RuntimeException()
        }

        val divide: BigDecimal = BigDecimal(it.value + "")
            .divide(BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_UP)
        return UTXO(
            address,
            it.txid,
            it.vout,
            //"",
            transaction[0]!!.vout[it.vout].scriptPubKey.hex,
            divide.toDouble(),
            it.height,
            it.confirmations
        )
    }

    private fun parse(it: TxRequestDTO): TransactionBean {
        val vinBeans = ArrayList<TransactionBean.VinBean>(it.vin.size)
        it.vin.forEach {
            vinBeans.add(TransactionBean.VinBean().apply {
                txid = it.txid
                vout = it.vout
                sequence = it.sequence
                scriptSig = TransactionBean.VinBean.ScriptSigBean().apply {
                    hex = it.hex
                }
            })
        }

        val voutBeans = ArrayList<TransactionBean.VoutBean>(it.vout.size)
        it.vout.forEach {
            voutBeans.add(TransactionBean.VoutBean().apply {
                n = it.n
                value = BigDecimal(it.value.toString() + "").divide(
                    BigDecimal("100000000"),
                    8,
                    BigDecimal.ROUND_HALF_UP
                ).toDouble()
                scriptPubKey = TransactionBean.VoutBean.ScriptPubKeyBean().apply {
                    asm = ""
                    hex = it.hex
                    reqSigs = 0
                    type = ""
                    addresses = it.addresses
                }
            })
        }

        return TransactionBean().apply {
            setBlockhash(it.blockHash)
            setBlocktime(it.blockTime)
            setConfirmations(it.confirmations)
            setHash(it.blockHash)
            setHex(it.hex)
            setTxid(it.txid)
            setVersion(it.version)
            vin = vinBeans
            vout = voutBeans
        }
    }
}