package com.violas.wallet.ui.applyForMint

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.ApplyManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.RefreshPageEvent
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.activity_apply_for_mint.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.palliums.violascore.wallet.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class ApplyForMintActivity
    : BaseAppActivity() {

    private val mApplyManager by lazy {
        ApplyManager()
    }

    private val mAccountManager by lazy {
        com.violas.wallet.biz.AccountManager()
    }
    private val mAccount by lazy {
        mAccountManager.currentAccount()
    }
    private val mTokenManager by lazy {
        TokenManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.hint_apply_for_mint)

        launch(Dispatchers.IO) {
            val applyStatus = mApplyManager.getApplyStatus(mAccount.address)
            withContext(Dispatchers.Main) {
                applyStatus?.data?.let { status ->
                    itemWalletAddress.setContent(mAccount.address)
                    itemTokenName.setContent(status.token_name)
                    itemTokenAmount.setContent(
                        BigDecimal(status.amount).divide(
                            BigDecimal("1000000"),
                            0,
                            RoundingMode.DOWN
                        ).stripTrailingZeros().toPlainString()
                    )

                    status.tokenIdx?.let {
                        val assertToken = AssertToken(
                            account_id = mAccount.id,
                            fullName = status.token_name,
                            name = status.token_name,
                            tokenIdx = it,
                            enable = true
                        )
                        btnMint.setOnClickListener {
                            submitMint(assertToken)
                        }
                    }
                }
            }
        }
    }

    private fun submitMint(assertToken: AssertToken) {
        publish(assertToken) {
            launch(Dispatchers.IO) {
                val changePublishStatus = mApplyManager.changePublishStatus(mAccount.address)
                withContext(Dispatchers.Main) {
                    dismissProgress()
                    if (changePublishStatus != null && changePublishStatus.errorCode == 2000) {
                        EventBus.getDefault().post(RefreshPageEvent())
                        EventBus.getDefault().post(SwitchAccountEvent())
                        finish()
                    }
                }
            }
        }
    }

    private fun publish(assertToken: AssertToken, success: () -> Unit) {
        PasswordInputDialog()
            .setConfirmListener { bytes, dialogFragment ->
                dialogFragment.dismiss()
                showProgress()
                launch(Dispatchers.IO) {
                    val decrypt = SimpleSecurity.instance(applicationContext)
                        .decrypt(bytes, mAccount.privateKey)
                    Arrays.fill(bytes, 0.toByte())
                    if (decrypt == null) {
                        dismissProgress()
                        showToast(R.string.hint_password_error)
                        return@launch
                    }

                    val account = Account(KeyPair.fromSecretKey(decrypt))
                    val tokenManager = TokenManager()
                    try {
                        if (tokenManager.isPublish(account.getAddress().toHex())) {
                            success.invoke()
                        } else {
                            tokenManager.publishToken(account)
                            EventBus.getDefault().post(RefreshBalanceEvent())
                            mTokenManager.insert(true, assertToken)
                            success.invoke()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            showToast(getString(R.string.hint_mint_condition_error))
                        }
                    }
                }
            }
            .show(supportFragmentManager)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_apply_for_mint
    }
}
