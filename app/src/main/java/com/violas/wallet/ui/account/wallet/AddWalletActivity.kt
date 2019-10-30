package com.violas.wallet.ui.account.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.base.BaseViewHolder
import com.violas.wallet.base.recycler.RecycleViewItemDivider
import com.violas.wallet.ui.account.AccountType
import com.violas.wallet.utils.DensityUtility
import kotlinx.android.synthetic.main.activity_add_wallet.*
import kotlinx.android.synthetic.main.item_add_wallet.*
import kotlinx.android.synthetic.main.item_add_wallet.view.*

/**
 * Created by elephant on 2019-10-30 16:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 添加钱包页面
 */
class AddWalletActivity : BaseActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_add_wallet
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.add_wallet_title)

        val datas = arrayListOf(
            AddWalletVo(
                AccountType.VIOLAS,
                R.mipmap.ic_label_violas,
                getString(R.string.coin_name_violas),
                getString(R.string.account_type_name_violas)
            ),
            AddWalletVo(
                AccountType.BTC,
                R.mipmap.ic_label_btc,
                getString(R.string.coin_name_bitcoin),
                getString(R.string.account_type_name_bitcoin)
            ),
            AddWalletVo(
                AccountType.LIBRA,
                R.mipmap.ic_label_libra,
                getString(R.string.coin_name_libra),
                getString(R.string.account_type_name_libra)
            )
        )

        vRecyclerView.addItemDecoration(
            RecycleViewItemDivider(
                this,
                DensityUtility.dp2px(this, 10),
                0,
                0,
                0,
                showFirstTop = true,
                onlyShowLastBottom = true
            )
        )
        vRecyclerView.adapter = AddWalletAdapter(datas, onItemClick = { accountType ->
            AddWalletDialog.newInstance(accountType)
                .show(supportFragmentManager, AddWalletDialog::class.java.name)
        })
    }

    data class AddWalletVo(
        @AccountType val accountType: Int,
        @DrawableRes val logoId: Int,
        val coinName: String,
        val accountTypeName: String
    )

    class AddWalletViewHolder(view: View, val onItemClick: (Int) -> Unit) :
        BaseViewHolder<AddWalletVo>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemIndex: Int, itemDate: AddWalletVo?) {
            itemDate?.let {
                itemView.ivLogo.setImageResource(it.logoId)
                itemView.tvCoinName.text = it.coinName
                itemView.tvAccountType.text = it.accountTypeName
            }
        }

        override fun onViewClick(view: View, itemIndex: Int, itemDate: AddWalletVo?) {
            itemDate?.let {
                onItemClick.invoke(it.accountType)
            }
        }
    }

    class AddWalletAdapter(var datas: ArrayList<AddWalletVo>, val onItemClick: (Int) -> Unit) :
        RecyclerView.Adapter<AddWalletViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddWalletViewHolder {
            return AddWalletViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_add_wallet,
                    parent,
                    false
                ), onItemClick
            )
        }

        override fun onBindViewHolder(holder: AddWalletViewHolder, position: Int) {
            holder.bind(position, datas[position])
        }

        override fun getItemCount(): Int {
            return datas.size
        }
    }
}