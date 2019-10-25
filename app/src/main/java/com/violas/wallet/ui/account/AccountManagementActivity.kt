package com.violas.wallet.ui.account

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.widget.GroupListLayout
import kotlinx.android.synthetic.main.activity_account_operation.*

/**
 * Created by elephant on 2019-10-23 17:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 钱包账户管理页面
 */
class AccountManagementActivity : BaseActivity() {

    companion object {
        private const val INTENT_KEY_OPERATION_MODE = "INTENT_KEY_OPERATION_MODE"
        private const val INTENT_KEY_DISPLAY_MODE = "INTENT_KEY_DISPLAY_MODE"
    }

    var operationMode: Int = AccountOperationMode.SELECTION
    var displayMode: Int = AccountDisplayMode.ALL

    override fun getLayoutResId(): Int {
        return R.layout.activity_account_operation
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.account_management_title)

        initView()
        initData()
    }


    private fun initView() {
        accOptGroupListLayout.itemFactory = object : GroupListLayout.ItemFactory() {

            override fun createGroupItemLayout(
                context: Context,
                viewType: Int
            ): GroupListLayout.ItemLayout<out GroupListLayout.ItemData> {
                return ContentItem(context)
            }
        }
    }

    private fun initData() {

    }

    class ContentItem(context: Context) : GroupListLayout.ItemLayout<AccountVo>,
        View.OnClickListener {

        private val rootView: View = View.inflate(context, R.layout.item_account_operation, null)
        private val tvName: TextView
        private val tvAddress: TextView

        var accountVo: AccountVo? = null

        init {
            tvName = rootView.findViewById(R.id.tvAccountName)
            tvAddress = rootView.findViewById(R.id.tvAccountAddress)
        }

        override fun refreshView(itemData: GroupListLayout.ItemData?) {
            accountVo = itemData as? AccountVo

            accountVo?.let {
                tvName.text = it.accountDO.walletNickname
                tvAddress.text = it.accountDO.address
                rootView.setBackgroundResource(
                    when (it.accountDO.coinNumber) {
                        CoinTypes.Libra.coinType() ->
                            R.mipmap.ic_account_management_libra
                        CoinTypes.Bitcoin.coinType() ->
                            R.mipmap.ic_account_management_btc
                        else ->
                            R.mipmap.ic_account_management_violas
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
                    // TODO 跳转到钱包管理页面
                }
            }
        }

    }
}