package org.palliums.libracore.admissioncontrol

import admission_control.AdmissionControlGrpc
import admission_control.AdmissionControlOuterClass
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.protobuf.ByteString
import com.smallraw.libardemo.grpcResponse.DecodeResponse
import io.grpc.Channel
import org.palliums.libracore.transaction.*
import org.palliums.libracore.utils.HexUtils
import org.palliums.libracore.wallet.Account
import types.GetWithProof
import types.TransactionOuterClass
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.Executors

class LibraAdmissionControl(private val mChannel: Channel) {
    private val mExecutor = Executors.newFixedThreadPool(4)
    private var mHandler = Handler(Looper.getMainLooper())

    fun getBalance(address: String, call: (amount: String) -> Unit) {
        getBalanceInMicroLibras(address) {
            mExecutor.execute {
                val toPlainString = BigDecimal(it.toString())
                    .divide(
                        BigDecimal("1000000"),
                        6,
                        RoundingMode.HALF_DOWN
                    )
                    .stripTrailingZeros()
                    .toPlainString()
                mHandler.post {
                    call.invoke(toPlainString)
                }
            }
        }
    }

    fun getBalanceInMicroLibras(address: String, call: (amount: Long) -> Unit) {
        getAccountStatus(address, { accountStatus ->
            if (accountStatus.accountStates.isEmpty()) {
                mHandler.post {
                    call.invoke(0)
                }

            } else {
                mHandler.post {
                    call.invoke(accountStatus.accountStates[0].balanceInMicroLibras)
                }
            }
        }, {
            mHandler.post {
                call.invoke(0)
            }
        })
    }

    fun sendTransaction(
        rawTransaction: RawTransaction,
        publicKey: ByteArray,
        signed: ByteArray,
        call: (success: Boolean) -> Unit
    ) {
        val signedTransaction = SignedTransaction(
            rawTransaction,
            publicKey,
            signed
        )

        val signedTransactionGrpc = TransactionOuterClass.SignedTransaction.newBuilder()
            .setTxnBytes(ByteString.copyFrom(signedTransaction.toByteArray()))
            .build()

        val submitTxReq =
            AdmissionControlOuterClass.SubmitTransactionRequest.newBuilder()
                .setTransaction(
                    signedTransactionGrpc
                )
                .build()

        try {
            val client = AdmissionControlGrpc.newBlockingStub(mChannel)
            val response = client.submitTransaction(submitTxReq)

            println(
                """
               发送状态
               AcStatus:${response.acStatus} 
               MempoolStatus:${response.mempoolStatus} 
               VmStatus:${response.vmStatus} 
               StatusCase:${response.statusCase} 
            """
            )

            mHandler.post {
                if (response.statusCase.toString() == "AC_STATUS") {
                    call.invoke(true)
                } else {
                    call.invoke(false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.invoke(false)
        }
    }

    fun sendCoin(
        context: Context,
        account: Account,
        address: String,
        amount: Long,
        call: (success: Boolean) -> Unit
    ) {
        val senderAddress = account.getAddress().toHex()
        getSequenceNumber(senderAddress, {sequenceNumber->

            val publishTokenPayload = TransactionPayload.optionTransactionPayload(
                context, address, amount
            )

            val rawTransaction = RawTransaction.optionTransaction(
                senderAddress,
                publishTokenPayload,
                sequenceNumber
            )

            sendTransaction(
                rawTransaction,
                account.keyPair.getPublicKey(),
                account.keyPair.sign(rawTransaction.toByteArray()),
                call
            )
        }, {
            call.invoke(false)
        })
    }

    fun getSequenceNumber(
        address: String,
        call: (sequenceNumber: Long) -> Unit,
        error: (Exception) -> Unit
    ) {
        mExecutor.execute {
            val client = AdmissionControlGrpc.newBlockingStub(mChannel)
            getAccountStatus(address, { accountStatus ->
                if (accountStatus.accountStates.isEmpty()) {
                    call.invoke(0)
                } else {
                    call.invoke(accountStatus.accountStates[0].sequenceNumber)
                }
            }, {
                error.invoke(it)
            })
        }
    }

    fun getAccountStatus(
        address: String,
        call: (UpdateToLatestLedgerResultBean) -> Unit,
        error: (Exception) -> Unit
    ) {
        mExecutor.execute {
            try {
                val client = AdmissionControlGrpc.newBlockingStub(mChannel)

                val sequenceNumberRequest =
                    GetWithProof.GetAccountStateRequest.newBuilder()
                        .setAddress(ByteString.copyFrom(HexUtils.fromHex(address)))
                        .build()

                val requestItem = GetWithProof.RequestItem.newBuilder()
                    .setGetAccountStateRequest(sequenceNumberRequest)
                    .build()

                val latestLedgerRequest = GetWithProof.UpdateToLatestLedgerRequest.newBuilder()
                    .addRequestedItems(requestItem)
                    .build()

                val updateToLatestLedgerResponse = client.updateToLatestLedger(latestLedgerRequest)
                val decode = DecodeResponse.decode(updateToLatestLedgerResponse)
                Log.e("=getAccountStatus=", decode.toString())
                call.invoke(decode)
            } catch (e: Exception) {
                e.printStackTrace()
                error.invoke(e)
            }
        }
    }
}