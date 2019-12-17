package com.violas.wallet.ui.account.selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.palliums.base.BaseViewHolder
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.ui.account.AccountType
import kotlinx.android.synthetic.main.activity_account_selection.*
import me.yokeyword.fragmentation.Fragmentation
import me.yokeyword.fragmentation.SupportFragment

/**
 * Created by elephant on 2019-10-25 16:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 钱包账户选择页面
 */
class AccountSelectionActivity : BaseAppActivity() {

    companion object {
        private const val EXTRA_KEY_LAST_ACCOUNT_TYPE = "EXTRA_KEY_LAST_ACCOUNT_TYPE"
    }

    lateinit var adapter: AccountLabelAdapter

    override fun getLayoutResId(): Int {
        return R.layout.activity_account_selection
    }

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_GREY_BACKGROUND
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Fragmentation.builder()
            // 显示悬浮球 ; 其他Mode:SHAKE: 摇一摇唤出   NONE：隐藏
            .stackViewMode(Fragmentation.BUBBLE)
            .debug(BuildConfig.DEBUG)
            .install()

        setTitle(R.string.account_selection_title)

        var lastAccountType = AccountType.ALL
        savedInstanceState?.let {
            lastAccountType = it.getInt(EXTRA_KEY_LAST_ACCOUNT_TYPE, AccountType.ALL)
        }

        adapter = AccountLabelAdapter(
            getLabels(lastAccountType),
            onSwitchLabel = { displayType ->
                // TODO 在显示悬浮分组 GroupListLayout.showFloatGroup = true 时，loadRootFragment
                //      和第一次start启动的AccountSelectionFragment的GroupListLayout会回调
                //      RecyclerView.onScrolled方法去处理悬浮分组，但后面start启动的AccountSelectionFragment
                //      的GroupListLayout不回调RecyclerView.onScrolled方法无法处理悬浮分组，导致上一
                //      次的悬浮标题还显示着，与本身分组标题显示不一致。
                val fragment = findFragment(AccountSelectionFragment::class.java)
                fragment.putNewBundle(AccountSelectionFragment.newBundle(displayType))
                (topFragment as SupportFragment).start(fragment, SupportFragment.SINGLETASK)
            })
        rv_wallet_account_navigation.adapter = adapter

        val fragment = findFragment(AccountSelectionFragment::class.java)
        fragment?.let {
            it.pop()
        }

        loadRootFragment(
            R.id.vp_wallet_account_container,
            AccountSelectionFragment.newInstance(lastAccountType)
        )
    }

    private fun getLabels(@AccountType lastAccountType: Int): ArrayList<AccountLabelVo> {
        return arrayListOf(
            AccountLabelVo(AccountType.ALL, lastAccountType == AccountType.ALL),
            AccountLabelVo(AccountType.VIOLAS, lastAccountType == AccountType.VIOLAS),
            AccountLabelVo(AccountType.LIBRA, lastAccountType == AccountType.LIBRA),
            AccountLabelVo(AccountType.BTC, lastAccountType == AccountType.BTC)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(EXTRA_KEY_LAST_ACCOUNT_TYPE, adapter.currentAccountType())
    }

    data class AccountLabelVo(
        @AccountType
        val accountType: Int,
        var selected: Boolean = false
    )

    class AccountLabelViewHolder(view: View, val onItemClick: (Int) -> Unit) :
        BaseViewHolder<AccountLabelVo>(view) {

        private val ivAccountLabel = itemView.findViewById<ImageView>(R.id.ivAccountLabel)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: AccountLabelVo?) {
            itemData?.let {
                ivAccountLabel.setImageResource(
                    when (it.accountType) {
                        AccountType.BTC -> {
                            if (it.selected)
                                R.drawable.selector_label_btc
                            else
                                R.drawable.selector_label_btc_unsel
                        }

                        AccountType.VIOLAS -> {
                            if (it.selected)
                                R.drawable.selector_label_violas
                            else
                                R.drawable.selector_label_violas_unsel
                        }

                        AccountType.LIBRA -> {
                            if (it.selected)
                                R.drawable.selector_label_libra
                            else
                                R.drawable.selector_label_libra_unsel
                        }

                        else -> {
                            if (it.selected)
                                R.drawable.selector_label_all
                            else
                                R.drawable.selector_label_all_unsel
                        }
                    }
                )
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: AccountLabelVo?) {
            itemData?.let {
                onItemClick.invoke(itemPosition)
            }
        }
    }

    class AccountLabelAdapter(
        var items: List<AccountLabelVo>,
        private val onSwitchLabel: (Int) -> Unit
    ) :
        RecyclerView.Adapter<AccountLabelViewHolder>() {

        private var currentPosition: Int = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountLabelViewHolder {
            return AccountLabelViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_account_label,
                    parent,
                    false
                ),
                onItemClick = { position ->
                    if (position != currentPosition) {
                        onSwitchLabel.invoke(items[position].accountType)

                        items[currentPosition].selected = false
                        items[position].selected = true
                        currentPosition = position
                        notifyDataSetChanged()
                    }
                }
            )
        }

        override fun onBindViewHolder(holder: AccountLabelViewHolder, position: Int) {
            holder.bind(position, items[position])
        }

        override fun getItemCount(): Int {
            return items.size
        }

        @AccountType
        fun currentAccountType(): Int {
            return items[currentPosition].accountType
        }
    }
}