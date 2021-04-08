package com.violas.wallet.walletconnect

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.palliums.utils.getString
import com.quincysx.crypto.CoinType
import com.violas.wallet.R
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.android.synthetic.main.view_wallet_connect_add_currency_to_account.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_bank.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_dex.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_mint.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_none.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_transfer.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ViewFillers {
    suspend fun getView(
        context: Context,
        viewGroupContent: ViewGroup,
        tvTitle: TextView,
        coinType: CoinType,
        isSend: Boolean,
        viewType: Int,
        viewData: String
    ): View? = withContext(Dispatchers.IO) {
        return@withContext when (viewType) {
            TransactionDataType.ADD_CURRENCY_TO_ACCOUNT.value -> {
                val data = Gson().fromJson(
                    viewData,
                    AddCurrencyToAccountData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_add_currency_to_account,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_add_currency_title)
                    view.tvAddCurrencyName.text = data.currency
                    view.tvAddGasFeesAmount.text = "0.00"
                    view.tvAddGasFeesUnit.text = if (coinType == getDiemCoinType())
                        org.palliums.libracore.common.CURRENCY_DEFAULT_CODE
                    else
                        org.palliums.violascore.common.CURRENCY_DEFAULT_CODE
                }
                view
            }

            TransactionDataType.PEER_TO_PEER_WITH_METADATA.value -> {
                val data = Gson().fromJson(
                    viewData,
                    DiemTransferData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_transfer,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_transfer_title)
                    view.tvTransferAmount.text = convertAmountToDisplayAmountStr(data.amount)
                    view.tvTransferAmountUnit.text = data.currency
                    view.tvTransferPayeeAddress.text = data.payeeAddress
                    view.tvTransferFeesLabel.setText(R.string.common_label_gas_fees_colon)
                    view.tvTransferFeesAmount.text = "0.00"
                    view.tvTransferFeesUnit.text = if (coinType == getDiemCoinType())
                        org.palliums.libracore.common.CURRENCY_DEFAULT_CODE
                    else
                        org.palliums.violascore.common.CURRENCY_DEFAULT_CODE
                }
                view
            }

            TransactionDataType.BITCOIN_TRANSFER.value -> {
                val data = Gson().fromJson(
                    viewData,
                    BitcoinTransferData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_transfer,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_transfer_title)
                    view.tvTransferAmount.text =
                        convertAmountToDisplayAmountStr(data.amount, coinType)
                    view.tvTransferAmountUnit.text = coinType.coinName()
                    view.tvTransferPayeeAddress.text = data.payeeAddress
                    view.tvTransferFeesLabel.setText(R.string.common_label_miner_fees_colon)
                    view.tvTransferFeesAmount.text = "0.00"
                    view.tvTransferFeesUnit.text = coinType.coinName()
                }
                view
            }

            TransactionDataType.VIOLAS_EXCHANGE_SWAP.value -> {
                val data = Gson().fromJson(
                    viewData,
                    ExchangeSwapData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_dex,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_swap_title)
                    view.tvDexAmountALabel.setText(R.string.wallet_connect_swap_label_from)
                    view.tvDexAmountA.text = convertAmountToDisplayAmountStr(data.amountIn)
                    view.tvDexAmountAUnit.text = data.currencyIn
                    view.tvDexAmountBLabel.setText(R.string.wallet_connect_swap_label_to)
                    view.tvDexAmountB.text = convertAmountToDisplayAmountStr(data.amountOutMin)
                    view.tvDexAmountBUnit.text = data.currencyOut
                    view.tvDexGasFeesAmount.text = "0.00"
                    view.tvDexGasFeesUnit.text = "VLS"
                }
                view
            }

            TransactionDataType.VIOLAS_EXCHANGE_ADD_LIQUIDITY.value -> {
                val data = Gson().fromJson(
                    viewData,
                    ExchangeAddLiquidityData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_dex,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_pool_title_add_liquidity)
                    view.tvDexAmountALabel.setText(R.string.wallet_connect_pool_label_input)
                    view.tvDexAmountA.text = convertAmountToDisplayAmountStr(data.amountADesired)
                    view.tvDexAmountAUnit.text = data.currencyA
                    view.tvDexAmountBLabel.setText(R.string.wallet_connect_pool_label_input)
                    view.tvDexAmountB.text = convertAmountToDisplayAmountStr(data.amountBDesired)
                    view.tvDexAmountBUnit.text = data.currencyB
                    view.tvDexGasFeesAmount.text = "0.00"
                    view.tvDexGasFeesUnit.text = "VLS"
                }
                view
            }

            TransactionDataType.VIOLAS_EXCHANGE_REMOVE_LIQUIDITY.value -> {
                val data = Gson().fromJson(
                    viewData,
                    ExchangeRemoveLiquidityData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_dex,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_pool_title_remove_liquidity)
                    view.tvDexAmountALabel.setText(R.string.wallet_connect_pool_label_output)
                    view.tvDexAmountA.text = convertAmountToDisplayAmountStr(data.amountAMin)
                    view.tvDexAmountAUnit.text = data.currencyA
                    view.tvDexAmountBLabel.setText(R.string.wallet_connect_pool_label_output)
                    view.tvDexAmountB.text = convertAmountToDisplayAmountStr(data.amountBMin)
                    view.tvDexAmountBUnit.text = data.currencyB
                    view.tvDexGasFeesAmount.text = "0.00"
                    view.tvDexGasFeesUnit.text = "VLS"
                }
                view
            }

            TransactionDataType.VIOLAS_BANK_DEPOSIT.value -> {
                val data = Gson().fromJson(
                    viewData,
                    BankDepositData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_bank,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_bank_title_deposit)
                    view.tvBankAmountLabel.setText(R.string.wallet_connect_bank_label_deposit_amount)
                    view.tvBankAmount.text = convertAmountToDisplayAmountStr(data.amount)
                    view.tvBankAmountUnit.text = data.currency
                    view.tvBankGasFeesAmount.text = "0.00"
                    view.tvBankGasFeesUnit.text = "VLS"
                }
                view
            }

            TransactionDataType.VIOLAS_BANK_REDEEM.value -> {
                val data = Gson().fromJson(
                    viewData,
                    BankRedeemData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_bank,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_bank_title_withdrawal)
                    view.tvBankAmountLabel.setText(R.string.wallet_connect_bank_label_withdrawal_amount)
                    if (data.amount > 0) {
                        view.tvBankAmount.text = convertAmountToDisplayAmountStr(data.amount)
                        view.tvBankAmountUnit.text = data.currency
                    } else {
                        view.tvBankAmount.visibility = View.GONE
                        view.tvBankAmountUnit.text = getString(
                            R.string.wallet_connect_bank_desc_withdrawal_all_format,
                            data.currency,
                            context
                        )
                    }
                    view.tvBankGasFeesAmount.text = "0.00"
                    view.tvBankGasFeesUnit.text = "VLS"
                }
                view
            }

            TransactionDataType.VIOLAS_BANK_BORROW.value -> {
                val data = Gson().fromJson(
                    viewData,
                    BankBorrowData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_bank,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_bank_title_borrow)
                    view.tvBankAmountLabel.setText(R.string.wallet_connect_bank_label_borrow_amount)
                    view.tvBankAmount.text = convertAmountToDisplayAmountStr(data.amount)
                    view.tvBankAmountUnit.text = data.currency
                    view.tvBankGasFeesAmount.text = "0.00"
                    view.tvBankGasFeesUnit.text = "VLS"
                }
                view
            }

            TransactionDataType.VIOLAS_BANK_REPAY_BORROW.value -> {
                val data = Gson().fromJson(
                    viewData,
                    BankRepayBorrowData::class.java
                )

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_bank,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_bank_title_repayment)
                    view.tvBankAmountLabel.setText(R.string.wallet_connect_bank_label_repayment_amount)
                    if (data.amount > 0) {
                        view.tvBankAmount.text = convertAmountToDisplayAmountStr(data.amount)
                        view.tvBankAmountUnit.text = data.currency
                    } else {
                        view.tvBankAmount.visibility = View.GONE
                        view.tvBankAmountUnit.text = getString(
                            R.string.wallet_connect_bank_desc_repayment_all_format,
                            data.currency,
                            context
                        )
                    }
                    view.tvBankGasFeesAmount.text = "0.00"
                    view.tvBankGasFeesUnit.text = "VLS"
                }
                view
            }

            TransactionDataType.VIOLAS_EXCHANGE_WITHDRAW_REWARD.value -> {
                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_mint,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_mint_title_extract_pool_reward)
                    view.tvMintAmountDesc.setText(R.string.wallet_connect_mint_desc_extract_pool_reward)
                    view.tvMintGasFeesAmount.text = "0.00"
                    view.tvMintGasFeesUnit.text = "VLS"
                }
                view
            }

            TransactionDataType.VIOLAS_BANK_WITHDRAW_REWARD.value -> {
                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_mint,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(R.string.wallet_connect_mint_title_extract_bank_reward)
                    view.tvMintAmountDesc.setText(R.string.wallet_connect_mint_desc_extract_bank_reward)
                    view.tvMintGasFeesAmount.text = "0.00"
                    view.tvMintGasFeesUnit.text = "VLS"
                }
                view
            }

            else -> {
                val data: String = GsonBuilder().setPrettyPrinting().create()
                    .toJson(JsonParser.parseString(viewData))

                val view = LayoutInflater.from(context).inflate(
                    R.layout.view_wallet_connect_none,
                    viewGroupContent,
                    false
                )

                withContext(Dispatchers.Main) {
                    tvTitle.setText(
                        if (isSend)
                            R.string.wallet_connect_txn_title
                        else
                            R.string.wallet_connect_sign_title
                    )
                    view.tvNoneTxnContent.text = data
                    view.tvNoneGasFeesAmount.text = "0.00"
                    view.tvNoneGasFeesUnit.text = when (coinType) {
                        getBitcoinCoinType() -> coinType.coinName()
                        getDiemCoinType() -> org.palliums.libracore.common.CURRENCY_DEFAULT_CODE
                        else -> org.palliums.violascore.common.CURRENCY_DEFAULT_CODE
                    }
                }
                view
            }
        }
    }
}