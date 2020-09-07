package com.violas.wallet.ui.bank.withdrawal

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.palliums.base.BaseActivity
import com.palliums.base.ViewController
import com.palliums.extensions.close
import com.palliums.extensions.getShowErrorMessage
import com.palliums.utils.CustomMainScope
import com.palliums.utils.DensityUtility
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.bank.BankManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.common.KEY_ONE
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
import java.math.BigDecimal

/**
 * Created by elephant on 2020/9/4 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 银行提款弹窗
 */
class BankWithdrawalDialog : DialogFragment(), ViewController, CoroutineScope by CustomMainScope() {

    companion object {
        fun newInstance(
            depositDetails: DepositDetailsDTO
        ): BankWithdrawalDialog {
            return BankWithdrawalDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_ONE, depositDetails)
                }
            }
        }
    }

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
            depositDetails = bundle.getParcelable(KEY_ONE) ?: return false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun initView() {
        tvCurrency.text = depositDetails.tokenModule
        tvAmount.text = "${convertAmountToDisplayAmountStr(
            depositDetails.availableAmount
        )} ${depositDetails.tokenModule}"

        etInputBox.post {
            val paddingRight = tvCurrency.width + DensityUtility.dp2px(requireContext(), 15)
            etInputBox.setPadding(
                etInputBox.paddingLeft,
                etInputBox.paddingTop,
                paddingRight,
                etInputBox.paddingBottom
            )
        }
    }

    private fun initEvent() {
        ivClose.setOnClickListener {
            close()
        }

        tvAll.setOnClickListener {
            etInputBox.setText(convertAmountToDisplayAmountStr(depositDetails.availableAmount))
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
            tvTips.setText(R.string.bank_withdrawal_hint_no_input)
            return
        }

        // 输入为0判断, 当作未输入判断
        val amount = convertDisplayAmountToAmount(inputAmountStr)
        if (amount <= BigDecimal.ZERO) {
            tvTips.setText(R.string.bank_withdrawal_hint_no_input)
            return
        }

        // 可提取数量不足
        if (amount > BigDecimal(depositDetails.availableAmount)) {
            tvTips.setText(R.string.bank_withdrawal_hint_insufficient_extractable_amount)
            return
        }

        launch {
            val accountDO = withContext(Dispatchers.IO) {
                accountManager.getIdentityByCoinType(CoinTypes.Violas.coinType())
            }
            if (accountDO == null) {
                showToast(getString(R.string.hint_bank_business_account_error))
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
                    it,
                    amount.toLong()
                )
            })
        }
    }

    private fun sendTransfer(
        account: AccountDO,
        mark: LibraTokenAssetsMark,
        pwd: String,
        amount: Long
    ) {
        launch {
            try {
                showProgress()
                bankManager.redeem(pwd.toByteArray(), account, mark, amount)
                dismissProgress()
                showToast(getString(R.string.tips_withdrawal_success))
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
                close()
            } catch (e: Exception) {
                showToast(e.getShowErrorMessage(false))
                dismissProgress()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_ONE, depositDetails)
    }

    override fun onStart() {
        dialog?.window?.let {
            val point = Point()
            it.windowManager.defaultDisplay.getSize(point)

            val attributes = it.attributes
            attributes.y = (point.y * 0.15).toInt()
            it.attributes = attributes

            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
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
        (activity as? BaseActivity)?.showProgress(msg)
    }

    override fun dismissProgress() {
        (activity as? BaseActivity)?.dismissProgress()
    }

    override fun showToast(@StringRes msgId: Int, duration: Int) {
        showToast(getString(msgId), duration)
    }

    override fun showToast(msg: String, duration: Int) {
        (activity as? BaseActivity)?.showToast(msg, duration)
    }
}