package com.violas.wallet.ui.account.selection

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.base.BaseFragment
import com.violas.wallet.base.recycler.RecycleViewItemDivider
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.SwitchAccountEvent
import com.violas.wallet.ui.account.AccountDisplayType
import com.violas.wallet.ui.account.AccountOperationMode
import com.violas.wallet.ui.account.AccountVo
import com.violas.wallet.ui.account.loadAccounts
import com.violas.wallet.utils.DensityUtility
import com.violas.wallet.widget.GroupListLayout
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
        private const val EXTRA_KEY_DISPLAY_TYPE = "EXTRA_KEY_DISPLAY_TYPE"
        private const val EXTRA_KEY_OPERATION_MODE = "EXTRA_KEY_OPERATION_MODE"

        fun newInstance(
            @AccountDisplayType displayType: Int,
            @AccountOperationMode operationMode: Int = AccountOperationMode.SELECTION
        ): AccountSelectionFragment {
            return AccountSelectionFragment().apply {
                arguments = newBundle(displayType, operationMode)
            }
        }

        fun newBundle(
            @AccountDisplayType displayType: Int,
            @AccountOperationMode operationMode: Int = AccountOperationMode.SELECTION
        ): Bundle {
            return Bundle().apply {
                putInt(EXTRA_KEY_DISPLAY_TYPE, displayType)
                putInt(EXTRA_KEY_OPERATION_MODE, operationMode)
            }
        }
    }

    private var displayType: Int = AccountDisplayType.ALL
    private var operationMode: Int = AccountOperationMode.SELECTION

    override fun getLayoutResId(): Int {
        return R.layout.fragment_account_selection
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        arguments?.let {
            displayType = it.getInt(EXTRA_KEY_DISPLAY_TYPE, AccountDisplayType.ALL)
            operationMode = it.getInt(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.SELECTION)
        }

        initView()
        initData()
    }

    override fun onNewBundle(args: Bundle?) {
        super.onNewBundle(args)

        args?.let {
            displayType = it.getInt(EXTRA_KEY_DISPLAY_TYPE, AccountDisplayType.ALL)
            operationMode = it.getInt(EXTRA_KEY_OPERATION_MODE, AccountOperationMode.SELECTION)
        }

        initData()
    }

    private fun initView() {
        accOptGroupListLayout.itemFactory = object : GroupListLayout.ItemFactory() {

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
        accOptGroupListLayout.addItemDecoration(
            RecycleViewItemDivider(
                requireContext(),
                DensityUtility.dp2px(context, 4),
                DensityUtility.dp2px(context, 60),
                0,
                0,
                showFirstTop = true,
                onlyShowLastBottom = true
            )
        )
    }

    private fun initData() {
        launch(Dispatchers.IO) {
            //val data = fakeAccounts(displayType)
            val data = loadAccounts(displayType)
            withContext(Dispatchers.Main) {
                accOptGroupListLayout.setData(data)
            }
        }
    }

    class TitleItem(context: Context, private val isFloat: Boolean) :
        GroupListLayout.ItemLayout<AccountVo> {

        private val tvTitle: TextView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(
                DensityUtility.dp2px(context, 18),
                DensityUtility.dp2px(context, if (isFloat) 19 else 15),
                DensityUtility.dp2px(context, 15),
                DensityUtility.dp2px(context, 8)
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
            setTextColor(ResourcesCompat.getColor(context.resources, R.color.account_group_title, null))
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
            return tvTitle
        }
    }

    inner class ContentItem(context: Context) : GroupListLayout.ItemLayout<AccountVo>,
        View.OnClickListener {

        private val rootView: View = View.inflate(context, R.layout.item_account_selection, null)
        private val tvName: TextView
        private val tvAddress: TextView
        private val ivSelected: ImageView

        private var accountVo: AccountVo? = null

        init {
            rootView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    DensityUtility.dp2px(context, 8),
                    0,
                    DensityUtility.dp2px(context, 15),
                    DensityUtility.dp2px(context, 4)
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
                tvName.text = it.accountDO.walletNickname
                tvAddress.text = it.accountDO.address
                ivSelected.visibility = if (it.selected) View.VISIBLE else View.GONE
                rootView.setBackgroundResource(
                    when (it.accountDO.coinNumber) {
                        CoinTypes.Libra.coinType() ->
                            R.drawable.selector_account_selection_item_libra
                        CoinTypes.Bitcoin.coinType(),
                        CoinTypes.BitcoinTest.coinType() ->
                            R.drawable.selector_account_selection_item_btc
                        else ->
                            R.drawable.selector_account_selection_item_violas
                    }
                )
            }
        }

        override fun getItemView(): View {
            return rootView
        }

        override fun onClick(view: View) {
            if (!BaseActivity.isFastMultiClick(view)) {
                accountVo?.let {
                    // 切换当前钱包账号
                    AccountManager().switchCurrentAccount(it.accountDO.id)
                    // 发送切换钱包账号事件
                    EventBus.getDefault().post(SwitchAccountEvent())
                    // 关闭当前页面
                    this@AccountSelectionFragment.finishActivity()
                }
            }
        }
    }
}