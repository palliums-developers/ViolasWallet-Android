package com.violas.wallet.ui.account.selection

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.palliums.base.BaseFragment
import com.palliums.utils.DensityUtility
import com.palliums.utils.getColor
import com.palliums.utils.isFastMultiClick
import com.palliums.widget.dividers.RecyclerViewItemDividers
import com.palliums.widget.groupList.GroupListLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.EXTRA_KEY_ACCOUNT_TYPE
import com.violas.wallet.common.EXTRA_KEY_OPERATION_MODE
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.account.AccountOperationMode
import com.violas.wallet.ui.account.AccountType
import com.violas.wallet.ui.account.AccountVo
import com.violas.wallet.ui.account.loadAccounts
import kotlinx.android.synthetic.main.fragment_account_selection.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2019-10-25 16:48.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 钱包账户选择组件视图
 */
class AccountSelectionFragment : BaseFragment() {

    companion object {
        fun newInstance(
            @AccountType accountType: Int,
            @AccountOperationMode operationMode: Int = AccountOperationMode.SWITCH
        ): AccountSelectionFragment {
            return AccountSelectionFragment().apply {
                arguments = newBundle(accountType, operationMode)
            }
        }

        fun newBundle(
            @AccountType accountType: Int,
            @AccountOperationMode operationMode: Int = AccountOperationMode.SWITCH
        ): Bundle {
            return Bundle().apply {
                putInt(EXTRA_KEY_ACCOUNT_TYPE, accountType)
                putInt(EXTRA_KEY_OPERATION_MODE, operationMode)
            }
        }
    }

    private val accountManager by lazy { AccountManager() }
    private val accountDao by lazy { DataRepository.getAccountStorage() }

    private var accountType: Int = AccountType.ALL
    private var operationMode: Int = AccountOperationMode.SWITCH

    override fun getLayoutResId(): Int {
        return R.layout.fragment_account_selection
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        arguments?.let {
            accountType = it.getInt(EXTRA_KEY_ACCOUNT_TYPE, AccountType.ALL)
            operationMode = it.getInt(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.SWITCH)
        }

        initView()
        initData()
    }

    override fun onNewBundle(args: Bundle?) {
        super.onNewBundle(args)

        args?.let {
            accountType = it.getInt(EXTRA_KEY_ACCOUNT_TYPE, AccountType.ALL)
            operationMode = it.getInt(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.SWITCH)
        }

        initData()
    }

    private fun initView() {
        vAccountList.showSlideBar(false)
        vAccountList.itemFactory = object : GroupListLayout.ItemFactory() {

            override fun createContentItemLayout(
                context: Context,
                viewType: Int
            ): GroupListLayout.ItemLayout<out GroupListLayout.ItemData> {
                return ContentItem(context)
            }

            override fun createTitleItemLayout(
                context: Context,
                isFloat: Boolean
            ): GroupListLayout.ItemLayout<out GroupListLayout.ItemData>? {
                return TitleItem(context, isFloat)
            }
        }
        vAccountList.addItemDecoration(
            RecyclerViewItemDividers(
                top = DensityUtility.dp2px(context, 10),
                bottom = DensityUtility.dp2px(context, 60),
                onlyShowLastBottom = true
            )
        )
    }

    private fun initData() {
        launch(Dispatchers.IO) {
            //val data = fakeAccounts(accountType)
            val data = loadAccounts(
                accountType, accountManager, accountDao
            )
            withContext(Dispatchers.Main) {
                vAccountList.setData(data)
            }
        }
    }

    class TitleItem(context: Context, private val isFloat: Boolean) :
        GroupListLayout.ItemLayout<AccountVo> {

        private val tvTitle: TextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
            setTextColor(getColor(R.color.account_group_title, context))
        }

        override fun refreshView(itemData: GroupListLayout.ItemData?) {
            if (itemData == null || itemData.getGroupName().isNullOrEmpty()) {
                tvTitle.visibility = View.GONE
            } else {
                tvTitle.visibility = View.VISIBLE
                tvTitle.text = itemData.getGroupName()
            }
        }

        override fun getItemView(): View {
            return tvTitle.apply {
                setPadding(
                    DensityUtility.dp2px(context, 18),
                    DensityUtility.dp2px(context, if (isFloat) 10 else 0),
                    DensityUtility.dp2px(context, 15),
                    DensityUtility.dp2px(context, 8)
                )
            }
        }
    }

    inner class ContentItem(context: Context) : GroupListLayout.ItemLayout<AccountVo>,
        View.OnClickListener {

        private val rootView: View = View.inflate(
            context, R.layout.item_account_selection, null
        )
        private val tvName: TextView
        private val tvAddress: TextView
        private val ivSelected: ImageView

        private var accountVo: AccountVo? = null

        init {
            rootView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    DensityUtility.dp2px(context, 8),
                    0,
                    DensityUtility.dp2px(context, 14),
                    0
                )
            }

            tvName = rootView.findViewById(R.id.tvAccountName)
            tvAddress = rootView.findViewById(R.id.tvAccountAddress)
            ivSelected = rootView.findViewById(R.id.ivAccountSelected)
            rootView.setOnClickListener(this)
        }

        override fun refreshView(itemData: GroupListLayout.ItemData?) {
            accountVo = itemData as? AccountVo

            accountVo?.let {
//                tvName.text = it.accountDO.walletNickname
                tvAddress.text = it.accountDO.address
                ivSelected.visibility = if (it.selected) View.VISIBLE else View.GONE
                rootView.setBackgroundResource(
                    when (it.accountDO.coinNumber) {
                        CoinTypes.Libra.coinType() ->
                            R.drawable.sel_bg_account_selection_libra
                        CoinTypes.Bitcoin.coinType(),
                        CoinTypes.BitcoinTest.coinType() ->
                            R.drawable.sel_bg_account_selection_bitcoin
                        else ->
                            R.drawable.sel_bg_account_selection_violas
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
                    if (operationMode == AccountOperationMode.SWITCH && !it.selected) {
                        // 切换当前钱包账号
                        accountManager.switchCurrentAccount(it.accountDO.id)
                        // 发送切换钱包账号事件
                        EventBus.getDefault().post(SwitchAccountEvent())
                    }
                    // 关闭当前页面
                    this@AccountSelectionFragment.finishActivity()
                }
            }
        }
    }
}