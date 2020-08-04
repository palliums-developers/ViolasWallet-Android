package com.violas.wallet.ui.managerAssert

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.listing.ListingViewModel
import com.palliums.utils.DensityUtility
import com.palliums.utils.getResourceId
import com.palliums.utils.openBrowser
import com.palliums.violas.bean.TokenMark
import com.palliums.widget.dividers.RecycleViewItemDividers
import com.quincysx.crypto.CoinTypes
import com.smallraw.support.switchcompat.SwitchButton
import com.violas.wallet.R
import com.violas.wallet.base.BaseListingActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertOriginateToken
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.web.WebCommonActivity
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.utils.loadRoundedImage
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsLibraCoinVo
import com.violas.wallet.widget.dialog.PublishTokenDialog
import kotlinx.android.synthetic.main.item_manager_assert.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManagerAssertActivity : BaseListingActivity<AssertOriginateToken>() {

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

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel?> create(modelClass: Class<T>): T {
                    return modelClass
                        .getConstructor(TokenManager::class.java)
                        .newInstance(mTokenManager)
                }
            }
        ).get(ViewModel::class.java)
    }

    private val viewAdapter by lazy {
        ViewAdapter { checkbox, checked, assertToken ->
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

    override fun getViewModel(): ListingViewModel<AssertOriginateToken> {
        return viewModel
    }

    override fun getViewAdapter(): ListingViewAdapter<AssertOriginateToken> {
        return viewAdapter
    }

    override fun enableRefresh(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_assert_manager)
        setTitleRightText(R.string.action_get_experience_the_coin)

        getRecyclerView().addItemDecoration(
            RecycleViewItemDividers(
                top = DensityUtility.dp2px(this, 5),
                bottom = DensityUtility.dp2px(this, 5),
                left = DensityUtility.dp2px(this, 16),
                right = DensityUtility.dp2px(this, 16),
                showFirstTop = true
            )
        )

        mListingHandler.init()
        getRefreshLayout()?.setOnRefreshListener {
            viewModel.execute()
        }
        viewModel.execute()
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
                    val hasSuccess = mTokenManager.publishToken(
                        assertOriginateToken.account_id,
                        it,
                        assertOriginateToken.tokenMark!!
                    )
                    if (hasSuccess) {
                        mTokenManager.insert(checked, assertOriginateToken)
                    } else {
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
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        super.onBackPressedSupport()
    }

    class ViewModel(
        private val tokenManager: TokenManager
    ) : ListingViewModel<AssertOriginateToken>() {

        override suspend fun loadData(vararg params: Any): List<AssertOriginateToken> {
            return tokenManager.loadSupportToken()
        }

        override fun checkNetworkBeforeExecute(): Boolean {
            return false
        }
    }

    class ViewAdapter(
        private val callbacks: (SwitchButton, Boolean, AssertOriginateToken) -> Unit
    ) : ListingViewAdapter<AssertOriginateToken>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<AssertOriginateToken> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_manager_assert,
                    parent,
                    false
                ),
                callbacks
            )
        }
    }

    class ViewHolder(
        view: View,
        private val callbacks: (SwitchButton, Boolean, AssertOriginateToken) -> Unit
    ) : BaseViewHolder<AssertOriginateToken>(view) {

        init {
            itemView.setOnClickListener(this)
            itemView.checkBox.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: AssertOriginateToken?) {
            itemData?.let {
                itemView.name.text = itemData.name
                itemView.fullName.text = itemData.fullName

                if (itemData.isToken) {
                    itemView.checkBox.visibility = View.VISIBLE
                    itemView.checkBox.setCheckedImmediatelyNoEvent(itemData.enable)
                } else {
                    itemView.checkBox.visibility = View.GONE
                }

                itemView.ivCoinLogo.loadRoundedImage(
                    itemData.logo,
                    getResourceId(R.attr.iconCoinDefLogo, itemView.context),
                    14
                )
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: AssertOriginateToken?) {
            itemData?.let {
                if (!it.isToken) return@let

                if (view == itemView) {
                    itemView.checkBox.isChecked = !itemView.checkBox.isChecked
                }
                callbacks.invoke(
                    itemView.checkBox,
                    itemView.checkBox.isChecked,
                    itemData
                )
            }
        }
    }
}
