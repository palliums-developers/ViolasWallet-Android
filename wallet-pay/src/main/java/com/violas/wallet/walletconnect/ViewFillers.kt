package com.violas.wallet.walletconnect

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.violas.wallet.R
import com.violas.wallet.walletconnect.messageHandler.TransferBitcoinDataType
import kotlinx.android.synthetic.main.view_wallet_connect_exchange_swap.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_none.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_publish.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_publish.view.tvDescribeSender
import kotlinx.android.synthetic.main.view_wallet_connect_transfer.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

class ViewFillers {
    suspend fun getView(
        context: Context,
        viewGroupContent: ViewGroup,
        viewType: Int,
        viewData: String
    ): View? = withContext(Dispatchers.IO) {
        return@withContext when (viewType) {
            TransactionDataType.None.value -> {
                val create = GsonBuilder().setPrettyPrinting().create()

                val je: JsonElement = JsonParser.parseString(viewData)
                val prettyJsonStr2: String = create.toJson(je)
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_wallet_connect_none, viewGroupContent, false)
                withContext(Dispatchers.Main) {
                    view.tvContent.text = prettyJsonStr2
                }
                view
            }
            TransactionDataType.PUBLISH.value -> {
                println("transfer data: $viewData")
                val mPublishDataType = Gson().fromJson(
                    viewData,
                    PublishDataType::class.java
                )
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_wallet_connect_publish, viewGroupContent, false)
                withContext(Dispatchers.Main) {
                    view.tvDescribeSender.text = mPublishDataType.form
                    view.tvDescribeCoinName.text = mPublishDataType.coinName
                }
                view
            }
            TransactionDataType.Transfer.value -> {
                println("transfer data: $viewData")
                val mTransferDataType = Gson().fromJson(
                    viewData,
                    TransferDataType::class.java
                )

                val amount = BigDecimal(mTransferDataType.amount).divide(
                    BigDecimal("1000000"), 6, RoundingMode.DOWN
                ).stripTrailingZeros().toPlainString()

                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_wallet_connect_transfer, viewGroupContent, false)
                withContext(Dispatchers.Main) {
                    view.tvDescribeSender.text = mTransferDataType.form
                    view.tvDescribeAddress.text = mTransferDataType.to
                    view.tvDescribeAmount.text = "$amount ${mTransferDataType.coinName}"
                    view.tvDescribeFee.text = "0.00 ${mTransferDataType.coinName}"
                }
                view
            }
            TransactionDataType.BITCOIN_TRANSFER.value -> {
                println("transfer data: $viewData")
                val mTransferDataType = Gson().fromJson(
                    viewData,
                    TransferBitcoinDataType::class.java
                )

                val amount = BigDecimal(mTransferDataType.amount).divide(
                    BigDecimal("100000000"), 8, RoundingMode.DOWN
                ).stripTrailingZeros().toPlainString()

                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_wallet_connect_transfer, viewGroupContent, false)
                withContext(Dispatchers.Main) {
                    view.tvDescribeSender.text = mTransferDataType.form
                    view.tvDescribeAddress.text = mTransferDataType.to
                    view.tvDescribeAmount.text = "$amount BTC"
                    view.tvDescribeFee.text = "0.00 BTC"
                    view.tvTitleFee.setText(R.string.common_label_miner_fees_colon)
                }
                view
            }
            TransactionDataType.VIOLAS_EXCHANGE_SWAP.value -> {
                println("transfer data: $viewData")
                val mPublishDataType = Gson().fromJson(
                    viewData,
                    ExchangeSwapDataType::class.java
                )
                val view = LayoutInflater.from(context)
                    .inflate(
                        R.layout.view_wallet_connect_exchange_swap,
                        viewGroupContent,
                        false
                    )
                withContext(Dispatchers.Main) {
                    view.tvDescribeSender.text = mPublishDataType.form
                    view.inputCoinAmount.text =
                        BigDecimal(mPublishDataType.amountIn).divide(
                            BigDecimal("1000000"), 6, RoundingMode.DOWN
                        ).stripTrailingZeros().toPlainString()
                    view.inputCoinUnit.text = mPublishDataType.inCoinName
                    view.outputCoinAmount.text =
                        BigDecimal(mPublishDataType.amountOutMin).divide(
                            BigDecimal("1000000"), 6, RoundingMode.DOWN
                        ).stripTrailingZeros().toPlainString()
                    view.outputCoinUnit.text = mPublishDataType.outCoinName
                    view.tvDescribePath.text =
                        mPublishDataType.path.joinToString(separator = ",")
                }
                view
            }
            TransactionDataType.VIOLAS_EXCHANGE_ADD_LIQUIDITY.value -> {
                println("transfer data: $viewData")
                val mPublishDataType = Gson().fromJson(
                    viewData,
                    ExchangeAddLiquidityDataType::class.java
                )
                val view = LayoutInflater.from(context)
                    .inflate(
                        R.layout.view_wallet_connect_exchange_add_liquidity,
                        viewGroupContent,
                        false
                    )
                withContext(Dispatchers.Main) {
                    view.tvDescribeSender.text = mPublishDataType.form
                    view.inputCoinAmount.text = BigDecimal(mPublishDataType.amountIn).divide(
                        BigDecimal("1000000"), 6, RoundingMode.DOWN
                    ).stripTrailingZeros().toPlainString()
                    view.inputCoinUnit.text = mPublishDataType.inCoinName
                    view.outputCoinAmount.text =
                        BigDecimal(mPublishDataType.amountOut).divide(
                            BigDecimal("1000000"), 6, RoundingMode.DOWN
                        ).stripTrailingZeros().toPlainString()
                    view.outputCoinUnit.text = mPublishDataType.outCoinName
                }
                view
            }
            TransactionDataType.VIOLAS_EXCHANGE_REMOVE_LIQUIDITY.value -> {
                println("transfer data: $viewData")
                val mPublishDataType = Gson().fromJson(
                    viewData,
                    ExchangeRemoveLiquidityDataType::class.java
                )
                val view = LayoutInflater.from(context)
                    .inflate(
                        R.layout.view_wallet_connect_exchange_remove_liquidity,
                        viewGroupContent,
                        false
                    )
                withContext(Dispatchers.Main) {
                    view.tvDescribeSender.text = mPublishDataType.form
                    view.inputCoinAmount.text = BigDecimal(mPublishDataType.amountInMin).divide(
                        BigDecimal("1000000"), 6, RoundingMode.DOWN
                    ).stripTrailingZeros().toPlainString()
                    view.inputCoinUnit.text = mPublishDataType.inCoinName
                    view.outputCoinAmount.text =
                        BigDecimal(mPublishDataType.amountOutMin).divide(
                            BigDecimal("1000000"), 6, RoundingMode.DOWN
                        ).stripTrailingZeros().toPlainString()
                    view.outputCoinUnit.text = mPublishDataType.outCoinName
                }
                view
            }
            TransactionDataType.VIOLAS_BANK_DEPOSIT.value -> {
                println("transfer data: $viewData")
                val mTransferDataType = Gson().fromJson(
                    viewData,
                    BankDepositDatatype::class.java
                )

                val amount = BigDecimal(mTransferDataType.amount).divide(
                    BigDecimal("1000000"), 6, RoundingMode.DOWN
                ).stripTrailingZeros().toPlainString()

                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_wallet_connect_bank_deposit, viewGroupContent, false)
                withContext(Dispatchers.Main) {
                    view.tvDescribeAmount.text = "$amount ${mTransferDataType.coinName}"
                    view.tvDescribeFee.text = "0.00 ${mTransferDataType.coinName}"
                }
                view
            }
            TransactionDataType.VIOLAS_BANK_REDEEM.value -> {
                println("transfer data: $viewData")
                val mTransferDataType = Gson().fromJson(
                    viewData,
                    BankDepositDatatype::class.java
                )

                val amount = BigDecimal(mTransferDataType.amount).divide(
                    BigDecimal("1000000"), 6, RoundingMode.DOWN
                ).stripTrailingZeros().toPlainString()

                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_wallet_connect_bank_redeem, viewGroupContent, false)
                withContext(Dispatchers.Main) {
                    view.tvDescribeAmount.text = "$amount ${mTransferDataType.coinName}"
                    view.tvDescribeFee.text = "0.00 ${mTransferDataType.coinName}"
                }
                view
            }
            TransactionDataType.VIOLAS_BANK_BORROW.value -> {
                println("transfer data: $viewData")
                val mTransferDataType = Gson().fromJson(
                    viewData,
                    BankDepositDatatype::class.java
                )

                val amount = BigDecimal(mTransferDataType.amount).divide(
                    BigDecimal("1000000"), 6, RoundingMode.DOWN
                ).stripTrailingZeros().toPlainString()

                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_wallet_connect_bank_borrow, viewGroupContent, false)
                withContext(Dispatchers.Main) {
                    view.tvDescribeAmount.text = "$amount ${mTransferDataType.coinName}"
                    view.tvDescribeFee.text = "0.00 ${mTransferDataType.coinName}"
                }
                view
            }
            TransactionDataType.VIOLAS_BANK_REPAY_BORROW.value -> {
                println("transfer data: $viewData")
                val mTransferDataType = Gson().fromJson(
                    viewData,
                    BankDepositDatatype::class.java
                )

                val amount = BigDecimal(mTransferDataType.amount).divide(
                    BigDecimal("1000000"), 6, RoundingMode.DOWN
                ).stripTrailingZeros().toPlainString()

                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_wallet_connect_bank_repay_borrow, viewGroupContent, false)
                withContext(Dispatchers.Main) {
                    view.tvDescribeAmount.text = "$amount ${mTransferDataType.coinName}"
                    view.tvDescribeFee.text = "0.00 ${mTransferDataType.coinName}"
                }
                view
            }
            else -> {
                null
            }
        }
    }
}