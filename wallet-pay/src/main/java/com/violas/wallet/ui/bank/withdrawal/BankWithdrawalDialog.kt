package com.violas.wallet.ui.bank.withdrawal

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.palliums.base.ViewController
import com.palliums.extensions.close
import com.palliums.extensions.getShowErrorMessage
import com.palliums.extensions.logError
import com.palliums.utils.CustomMainScope
import com.palliums.utils.DensityUtility
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.common.KEY_TWO
import com.violas.wallet.event.BankWithdrawalEvent
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bank.DepositDetailsDTO
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.convertAmountToDisplayAmountStr
import com.violas.wallet.utils.convertDisplayAmountToAmount
import kotlinx.android.synthetic.main.dialog_bank_withdrawal.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.palliums.violascore.http.ViolasException
import java.math.BigDecimal

/**
 * Created by elephant on 2020/9/4 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行提款弹窗
 */
class BankWithdrawalDialog : DialogFragment(), ViewController, CoroutineScope by CustomMainScope() {

    companion object {
        private const val TAG = "BankWithdrawalDialog"

        fun newInstance(
            productId: String,
            depositDetails: DepositDetailsDTO
        ): BankWithdrawalDialog {
            return BankWithdrawalDialog().apply {
                arguments = Bundle().apply {
                    putString(KEY_ONE, productId)
                    putParcelable(KEY_TWO, depositDetails)
                }
            }
        }
    }

    private lateinit var productId: String
    private lateinit var depositDetails: DepositDetailsDTO

    private val accountManager by lazy { AccountManager() }
    private val bankManager by lazy { BankManager() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_bank_withdrawal, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (initData(savedInstanceState)) {
            initView()
            initEvent()
        } else {
            close()
        }
    }

    private fun initData(savedInstanceState: Bundle?): Boolean {
        try {
            val bundle = savedInstanceState ?: arguments ?: return false
            productId = bundle.getString(KEY_ONE) ?: return false
            depositDetails = bundle.getParcelable(KEY_TWO) ?: return false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun initView() {
        tvCurrency.text = depositDetails.tokenDisplayName
        tvAmount.text = "${
            convertAmountToDisplayAmountStr(
                depositDetails.availableAmount
            )
        } ${depositDetails.tokenDisplayName}"

        etInputBox.post {
            etInputBox.setPadding(
                etInputBox.paddingLeft,
                etInputBox.paddingTop,
                tvCurrency.width + DensityUtility.dp2px(requireContext(), 15),
                etInputBox.paddingBottom
            )
        }
    }

    private fun initEvent() {
        ivClose.setOnClickListener {
            close()
        }

        tvAll.setOnClickListener {
            convertAmountToDisplayAmountStr(depositDetails.availableAmount).let {
                etInputBox.setText(it)
                etInputBox.setSelection(it.length)
            }
        }

        btnExtraction.setOnClickListener {
            clickExecExtraction()
        }

        etInputBox.addTextChangedListener {
            if (!tvTips.text.isNullOrBlank() && !it?.toString().isNullOrBlank()) {
                tvTips.text = ""
            }
        }
    }

    private fun clickExecExtraction() {
        // 未输入判断
        val inputAmountStr = etInputBox.text.toString().trim()
        if (inputAmountStr.isEmpty()) {
            tvTips.setText(R.string.bank_withdrawal_tips_withdrawal_amount_empty)
            return
        }

        // 输入为0判断, 当作未输入判断
        val amount = convertDisplayAmountToAmount(inputAmountStr)
        if (amount <= BigDecimal.ZERO) {
            tvTips.setText(R.string.bank_withdrawal_tips_withdrawal_amount_empty)
            return
        }

        // 可提取数量不足
        if (amount > BigDecimal(depositDetails.availableAmount)) {
            tvTips.setText(R.string.bank_withdrawal_tips_insufficient_balance)
            return
        }

        // 因为银行清算服务的原因，要想全部提取填0
        val realAmount = if (amount == BigDecimal(depositDetails.availableAmount))
            0
        else
            amount.toLong()

        launch {
            val accountDO = withContext(Dispatchers.IO) {
                accountManager.getIdentityByCoinType(CoinTypes.Violas.coinType())
            }
            if (accountDO == null) {
                showToast(getString(R.string.common_tips_account_error))
                return@launch
            }

            authenticateAccount(accountDO, accountManager, passwordCallback = {
                sendTransfer(
                    accountDO,
                    LibraTokenAssetsMark(
                        CoinTypes.Violas,
                        depositDetails.tokenModule,
                        depositDetails.tokenAddress,
                        depositDetails.tokenName
                    ),
                    it.toByteArray(),
                    realAmount
                )
            })
        }
    }

    private fun sendTransfer(
        account: AccountDO,
        mark: LibraTokenAssetsMark,
        pwd: ByteArray,
        amount: Long
    ) {
        launch {
            try {
                showProgress()
                withContext(Dispatchers.IO) {
                    bankManager.redeem(pwd, account, productId, mark, amount)
                }
                dismissProgress()
                showToast(getString(R.string.bank_withdrawal_tips_withdrawal_success))
                EventBus.getDefault().post(BankWithdrawalEvent())
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                close()
            } catch (e: Exception) {
                logError(e, TAG) { "withdrawal failure" }
                dismissProgress()
                showToast(
                    if (e is ViolasException.AccountNoActivation) {
                        getString(R.string.exception_account_no_activation)
                    } else {
                        e.getShowErrorMessage(false)
                    }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ONE, productId)
        outState.putParcelable(KEY_TWO, depositDetails)
    }

    override fun onStart() {
        dialog?.window?.let {
            val point = Point()
            it.windowManager.defaultDisplay.getSize(point)

            val attributes = it.attributes
            attributes.y = (point.y * 0.16).toInt()
            it.attributes = attributes

            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }

        dialog?.setCancelable(true)
        dialog?.setCanceledOnTouchOutside(false)

        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            it.isFocusableInTouchMode = true
            it.requestFocus()
            it.setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    close()
                    return@setOnKeyListener true
                }

                return@setOnKeyListener false
            }
        }
    }

    override fun showProgress(@StringRes resId: Int) {
        showProgress(getString(resId))
    }

    override fun showProgress(msg: String?) {
        (activity as? ViewController)?.showProgress(msg)
    }

    override fun dismissProgress() {
        (activity as? ViewController)?.dismissProgress()
    }

    override fun showToast(@StringRes msgId: Int, duration: Int) {
        showToast(getString(msgId), duration)
    }

    override fun showToast(msg: String, duration: Int) {
        (activity as? ViewController)?.showToast(msg, duration)
    }
}