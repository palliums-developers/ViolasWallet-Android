package com.violas.wallet.ui.managerAssert

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.palliums.utils.DensityUtility
import com.palliums.widget.dividers.RecycleViewItemDividers
import com.quincysx.crypto.CoinTypes
import com.smallraw.support.switchcompat.SwitchButton
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.event.RefreshBalanceEvent
import com.violas.wallet.event.TokenPublishEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.widget.dialog.PasswordInputDialog
import com.violas.wallet.widget.dialog.PublishTokenDialog
import kotlinx.android.synthetic.main.activity_manager_assert.*
import kotlinx.android.synthetic.main.item_manager_assert.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account
import java.util.*

class ManagerAssertActivity : BaseAppActivity() {
    override fun getLayoutResId() = R.layout.activity_manager_assert

    companion object {
        private const val EXT_ACCOUNT_ID = "0"
        fun start(context: Fragment, accountId: Long, requestId: Int) {
            val intent = Intent(context.activity, ManagerAssertActivity::class.java)
            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, requestId)
        }

        fun start(context: Activity, accountId: Long, requestId: Int) {
            val intent = Intent(context, ManagerAssertActivity::class.java)
            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, requestId)
        }
    }

    private lateinit var mAccount: AccountDO
    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }
    private val mSupportTokens = mutableListOf<AssertToken>()
    private val mAdapter by lazy {
        MyAdapter(mSupportTokens) { checkbox, checked, assertToken ->
            if (checked) {
                openToken(checkbox, checked, assertToken)
            } else {
                launch(Dispatchers.IO) {
                    mTokenManager.insert(checked, assertToken)
                }
            }
        }
    }

    private fun openToken(checkbox: SwitchButton, checked: Boolean, assertToken: AssertToken) {
        launch {
            if (assertToken.netEnable) {
                withContext(Dispatchers.IO) {
                    mTokenManager.insert(checked, assertToken)
                }
                checkbox.isChecked = true
            } else {
                PublishTokenDialog().setConfirmListener {
                    showPasswordDialog(assertToken, checkbox, checked)
                    it.dismiss()
                }.setCancelListener {
                    checkbox.isChecked = false
                }.show(supportFragmentManager)
            }
        }
    }

    private fun showPasswordDialog(
        assertToken: AssertToken,
        checkbox: SwitchButton,
        checked: Boolean
    ) {
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
                        this@ManagerAssertActivity.runOnUiThread {
                            checkbox.isChecked = false
                        }
                        return@launch
                    }
                    DataRepository.getViolasService()
                        .publishToken(
                            applicationContext,
                            Account(KeyPair.fromSecretKey(decrypt)),
                            assertToken.tokenAddress
                        ) {
                            dismissProgress()
                            if (!it) {
                                this@ManagerAssertActivity.runOnUiThread {
                                    checkbox.isChecked = false
                                    Toast.makeText(
                                        this@ManagerAssertActivity, String.format(
                                            getString(R.string.hint_not_none_coin_or_net_error),
                                            CoinTypes.Violas.coinName()
                                        ), Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                EventBus.getDefault().post(TokenPublishEvent())
                                EventBus.getDefault().post(RefreshBalanceEvent())
                                mTokenManager.insert(checked, assertToken)
                            }
                        }
                }
            }
            .setCancelListener {
                checkbox.isChecked = false
            }
            .show(supportFragmentManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_assert_manager)

        recyclerView.addItemDecoration(
            RecycleViewItemDividers(
                top = DensityUtility.dp2px(this, 5),
                bottom = DensityUtility.dp2px(this, 5),
                left = DensityUtility.dp2px(this, 16),
                right = DensityUtility.dp2px(this, 16)
            )
        )
        recyclerView.adapter = mAdapter

        showProgress()
        launch(Dispatchers.IO + handler) {
            mSupportTokens.clear()
            val currentAccountLong = intent.getLongExtra(EXT_ACCOUNT_ID, -1)
            mAccount = mAccountManager.getAccountById(currentAccountLong)
            mSupportTokens.addAll(mTokenManager.loadSupportToken(mAccount))
            withContext(Dispatchers.Main) {
                mAdapter.notifyDataSetChanged()
                dismissProgress()
            }
        }
    }

    override fun onBackPressedSupport() {
        setResult(Activity.RESULT_OK)
        super.onBackPressedSupport()
    }
}

class MyAdapter(
    val data: List<AssertToken>,
    private val callbacks: (SwitchButton, Boolean, AssertToken) -> Unit
) :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_manager_assert,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = data[position]
        holder.itemView.name.text = itemData.name
        holder.itemView.fullName.text = itemData.fullName
        if (itemData.isToken) {
            holder.itemView.checkBox.visibility = View.VISIBLE
            holder.itemView.checkBox.isChecked = itemData.enable
        } else {
            holder.itemView.checkBox.visibility = View.GONE
        }
        holder.itemView.ivCoinLogo.setImageResource(itemData.logo)

        holder.itemView.setOnClickListener { view ->
            if (itemData.isToken) {
                holder.itemView.checkBox.isChecked = !holder.itemView.checkBox.isChecked
            }
        }
        holder.itemView.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            callbacks.invoke(holder.itemView.checkBox, isChecked, itemData)
        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}
