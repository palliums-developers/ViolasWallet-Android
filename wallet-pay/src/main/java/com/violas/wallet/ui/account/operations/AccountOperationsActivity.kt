package com.violas.wallet.ui.account.operations

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.palliums.utils.DensityUtility
import com.palliums.utils.isFastMultiClick
import com.palliums.utils.start
import com.palliums.widget.dividers.RecycleViewItemDividers
import com.palliums.widget.groupList.GroupListLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_ID
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_TYPE
import com.violas.wallet.common.EXTRA_KEY_OPERATION_MODE
import com.violas.wallet.event.ChangeAccountNameEvent
import com.violas.wallet.event.WalletChangeEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.account.AccountOperationMode
import com.violas.wallet.ui.account.AccountType
import com.violas.wallet.ui.account.AccountVo
import com.violas.wallet.ui.account.loadAccounts
import com.violas.wallet.ui.account.wallet.AddWalletActivity
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import kotlinx.android.synthetic.main.activity_account_operations.*
import kotlinx.android.synthetic.main.item_account_operations.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by elephant on 2019-10-23 17:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 钱包账户操作页面，可实现管理和选择功能
 */
class AccountOperationsActivity : BaseAppActivity() {

    companion object {
        fun manageAccount(context: Context) {
            Intent(context, AccountOperationsActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_ACCOUNT_TYPE, AccountType.ALL)
                    putExtra(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.MANAGEMENT)
                }
                .start(context)
        }

        fun selectAccount(activity: Activity, requestCode: Int, @AccountType accountType: Int) {
            Intent(activity, AccountOperationsActivity::class.java)
                .apply {
                    putExtra(EXTRA_KEY_ACCOUNT_TYPE, accountType)
                    putExtra(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.SELECTION)
                }
                .start(activity, requestCode)
        }

        fun selectAccount(fragment: Fragment, requestCode: Int, @AccountType accountType: Int) {
            fragment.activity?.let {
                Intent(it, AccountOperationsActivity::class.java)
                    .apply {
                        putExtra(EXTRA_KEY_ACCOUNT_TYPE, accountType)
                        putExtra(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.SELECTION)
                    }
                    .start(fragment, requestCode)
            }
        }
    }

    private val accountManager by lazy { AccountManager() }
    private val accountDao by lazy { DataRepository.getAccountStorage() }

    private var accountType: Int = AccountType.ALL
    private var operationMode: Int = AccountOperationMode.MANAGEMENT

    override fun getLayoutResId(): Int {
        return R.layout.activity_account_operations
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initData(savedInstanceState)
        initView()
        loadData()
    }

    override fun onTitleRightViewClick() {
        AddWalletActivity.start(this)
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_KEY_ACCOUNT_TYPE, accountType)
        outState.putInt(EXTRA_KEY_OPERATION_MODE, operationMode)
    }

    private fun initData(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            accountType = savedInstanceState.getInt(EXTRA_KEY_ACCOUNT_TYPE, AccountType.ALL)
            operationMode =
                savedInstanceState.getInt(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.MANAGEMENT)
        } else if (intent != null) {
            accountType = intent.getIntExtra(EXTRA_KEY_ACCOUNT_TYPE, AccountType.ALL)
            operationMode =
                intent.getIntExtra(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.MANAGEMENT)
        }
    }

    private fun initView() {
        if (operationMode == AccountOperationMode.MANAGEMENT) {
            EventBus.getDefault().register(this)
            setTitle(R.string.account_management_title)
            setTitleRightImageResource(R.drawable.icon_add_address)
        } else {
            setTitle(R.string.account_selection_title)
        }

        vAccountList.showSlideBar(false)
        vAccountList.itemFactory = object : GroupListLayout.ItemFactory() {

            override fun createContentItemLayout(
                context: Context,
                viewType: Int
            ): GroupListLayout.ItemLayout<out GroupListLayout.ItemData> {
                return ContentItem(context, operationMode == AccountOperationMode.MANAGEMENT) {
                    when (operationMode) {
                        AccountOperationMode.MANAGEMENT -> {
                            WalletManagerActivity.start(
                                this@AccountOperationsActivity, it.accountDO.id
                            )
                        }

                        AccountOperationMode.SELECTION -> {
                            val intent = Intent().apply {
                                putExtra(EXTRA_KEY_ACCOUNT_ID, it.accountDO.id)
                            }
                            setResult(Activity.RESULT_OK, intent)
                            close()
                        }
                    }
                }
            }
        }
        vAccountList.addItemDecoration(
            RecycleViewItemDividers(
                top = DensityUtility.dp2px(this, 12),
                bottom = DensityUtility.dp2px(this, 80),
                showFirstTop = true,
                onlyShowLastBottom = true
            )
        )
    }

    private fun loadData() {
        launch(Dispatchers.IO) {
            //val data = fakeAccounts(AccountType.ALL)
            val data = loadAccounts(
                accountType, accountManager, accountDao, true
            )
            withContext(Dispatchers.Main) {
                vAccountList.setData(data)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWalletChangeEvent(event: WalletChangeEvent) {
        loadData()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChangeAccountNameEvent(event: ChangeAccountNameEvent) {
        loadData()
    }

    class ContentItem(
        context: Context,
        showOptView: Boolean,
        private val callback: (AccountVo) -> Unit
    ) : GroupListLayout.ItemLayout<AccountVo>, View.OnClickListener {

        private val rootView: View = View.inflate(
            context, R.layout.item_account_operations, null
        )
        private var accountVo: AccountVo? = null

        init {
            rootView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    DensityUtility.dp2px(context, 15),
                    0,
                    DensityUtility.dp2px(context, 15),
                    0
                )
            }
            rootView.ivAccountOpt.visibility = if (showOptView) View.VISIBLE else View.GONE
            rootView.setOnClickListener(this)
        }

        override fun refreshView(itemData: GroupListLayout.ItemData?) {
            accountVo = itemData as? AccountVo

            accountVo?.let {
                rootView.tvAccountName.text = it.accountDO.walletNickname
                rootView.tvAccountAddress.text = it.accountDO.address
                rootView.setBackgroundResource(
                    when (it.accountDO.coinNumber) {
                        CoinTypes.Libra.coinType() ->
                            R.drawable.sel_bg_account_management_libra
                        CoinTypes.Bitcoin.coinType(),
                        CoinTypes.BitcoinTest.coinType() ->
                            R.drawable.sel_bg_account_management_bitcoin
                        else ->
                            R.drawable.sel_bg_account_management_violas
                    }
                )
            }
        }

        override fun getItemView(): View {
            return rootView
        }

        override fun onClick(view: View) {
            if (!isFastMultiClick(view)) {
                accountVo?.let {
                    callback.invoke(it)
                }
            }
        }

    }
}