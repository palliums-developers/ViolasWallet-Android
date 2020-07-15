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
import com.bumptech.glide.Glide
import com.palliums.utils.DensityUtility
import com.palliums.utils.getResourceId
import com.palliums.utils.openBrowser
import com.palliums.violas.bean.TokenMark
import com.palliums.widget.dividers.RecycleViewItemDividers
import com.quincysx.crypto.CoinTypes
import com.smallraw.support.switchcompat.SwitchButton
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.web.WebCommonActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsLibraCoinVo
import com.violas.wallet.widget.dialog.PublishTokenDialog
import kotlinx.android.synthetic.main.activity_manager_assert.*
import kotlinx.android.synthetic.main.item_manager_assert.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManagerAssertActivity : BaseAppActivity() {
    override fun getLayoutResId() = R.layout.activity_manager_assert

    companion object {
        //        private const val EXT_ACCOUNT_ID = "0"
        fun start(context: Fragment, requestId: Int) {
            val intent = Intent(context.activity, ManagerAssertActivity::class.java)
//            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, requestId)
        }

        fun start(context: Activity, requestId: Int) {
            val intent = Intent(context, ManagerAssertActivity::class.java)
//            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, requestId)
        }
    }

    private var mChange = false

    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(this)
    }

    private val mSupportTokens = mutableListOf<AssertOriginateToken>()
    private val mAdapter by lazy {
        MyAdapter(mSupportTokens) { checkbox, checked, assertToken ->
            if (checked) {
                openToken(checkbox, checked, assertToken)
            } else {
                launch(Dispatchers.IO) {
                    mTokenManager.insert(checked, assertToken)
                }
                mChange = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_assert_manager)
        setTitleRightText(R.string.action_get_experience_the_coin)

        recyclerView.addItemDecoration(
            RecycleViewItemDividers(
                top = DensityUtility.dp2px(this, 5),
                bottom = DensityUtility.dp2px(this, 5),
                left = DensityUtility.dp2px(this, 16),
                right = DensityUtility.dp2px(this, 16),
                showFirstTop = true
            )
        )
        recyclerView.adapter = mAdapter

        showProgress()
        launch(Dispatchers.IO + handler) {
            mSupportTokens.clear()
            mSupportTokens.addAll(mTokenManager.loadSupportToken())
            withContext(Dispatchers.Main) {
                mAdapter.notifyDataSetChanged()
                dismissProgress()
            }
        }
    }

    private suspend fun isPublish(
        accountId: Long,
        tokenMark: TokenMark?
    ): Boolean {
        if (tokenMark == null) {
            return false
        }
        return mTokenManager.isPublish(accountId, tokenMark)
    }

    private fun openToken(
        checkbox: SwitchButton,
        checked: Boolean,
        assertOriginateToken: AssertOriginateToken
    ) {
        showProgress()
        launch(Dispatchers.IO) {
            try {
                if (isPublish(assertOriginateToken.account_id, assertOriginateToken.tokenMark)) {
                    mTokenManager.insert(checked, assertOriginateToken)
                    dismissProgress()
                    mChange = true
                } else {
                    val account = mAccountManager.getAccountById(assertOriginateToken.account_id)
                    dismissProgress()
                    withContext(Dispatchers.Main) {
                        PublishTokenDialog().setConfirmListener {
                            showPasswordDialog(account, assertOriginateToken, checkbox, checked)
                            it.dismiss()
                        }.setCancelListener {
                            checkbox.setCheckedNoEvent(false)
                        }.show(supportFragmentManager)
                    }
                }
            } catch (e: Exception) {
                dismissProgress()
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    checkbox.setCheckedNoEvent(false)
                    e.message?.let {
                        showToast(it)
                    }
                }
            }
        }
    }

    private fun showPasswordDialog(
        account: AccountDO,
        assertOriginateToken: AssertOriginateToken,
        checkbox: SwitchButton,
        checked: Boolean
    ) {
        authenticateAccount(
            account,
            mAccountManager,
            cancelCallback = {
                checkbox.setCheckedNoEvent(false)
            }
        ) {
            launch(Dispatchers.IO) {
                try {
                    mChange = true
                    mTokenManager.publishToken(
                        assertOriginateToken.account_id,
                        it,
                        assertOriginateToken.tokenMark!!
                    )
                    mTokenManager.insert(checked, assertOriginateToken)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        checkbox.setCheckedNoEvent(false)
                        showToast(
                            getString(
                                R.string.hint_not_none_coin_or_net_error,
                                CoinTypes.Violas.coinName()
                            ),
                            Toast.LENGTH_LONG
                        )
                    }
                }
                dismissProgress()
            }
        }
    }

    override fun onTitleRightViewClick() {
        var address = ""
        mWalletAppViewModel.mAssetsListLiveData.value?.forEach {
            if (it is AssetsLibraCoinVo) {
                address = it.address
                return@forEach
            }
        }

        val url = BaseBrowserUrl.getViolasTestCoinUrl(address)
        if (!openBrowser(this, url)) {
            WebCommonActivity.start(
                this,
                url,
                getString(R.string.action_get_experience_the_coin)
            )
        }
    }

    override fun onBackPressedSupport() {
        if (mChange) {
            setResult(Activity.RESULT_OK)
        }else{
            setResult(Activity.RESULT_CANCELED)
        }
        super.onBackPressedSupport()
    }
}

class MyAdapter(
    val data: List<AssertOriginateToken>,
    private val callbacks: (SwitchButton, Boolean, AssertOriginateToken) -> Unit
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
            holder.itemView.checkBox.setCheckedImmediatelyNoEvent(itemData.enable)
        } else {
            holder.itemView.checkBox.visibility = View.GONE
        }

        val defLogoResId =
            getResourceId(R.attr.walletHomeDefTokenLogo, holder.itemView.context)
        Glide.with(holder.itemView.context)
            .load(itemData.logo)
            .error(defLogoResId)
            .placeholder(defLogoResId)
            .into(holder.itemView.ivCoinLogo)

        holder.itemView.setOnClickListener { view ->
            if (itemData.isToken) {
                holder.itemView.checkBox.isChecked = !holder.itemView.checkBox.isChecked
                callbacks.invoke(
                    holder.itemView.checkBox,
                    holder.itemView.checkBox.isChecked,
                    itemData
                )
            }
        }
        holder.itemView.checkBox.setOnClickListener { view ->
            if (itemData.isToken) {
                callbacks.invoke(
                    holder.itemView.checkBox,
                    holder.itemView.checkBox.isChecked,
                    itemData
                )
            }
        }
//        holder.itemView.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
//            callbacks.invoke(holder.itemView.checkBox, isChecked, itemData)
//        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}
