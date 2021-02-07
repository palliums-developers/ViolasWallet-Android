package com.violas.wallet.ui.account.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.listing.ListingViewModel
import com.palliums.utils.DensityUtility
import com.palliums.utils.start
import com.palliums.widget.dividers.RecyclerViewItemDividers
import com.violas.wallet.R
import com.violas.wallet.base.BaseListingActivity
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.ui.account.AccountType
import kotlinx.android.synthetic.main.item_add_wallet.view.*

/**
 * Created by elephant on 2019-10-30 16:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 添加钱包页面
 */
class AddWalletActivity : BaseListingActivity<AddWalletVo>() {

    companion object {
        const val REQUEST_CREATE_IMPORT = 1

        fun start(context: Activity) {
            Intent(context, AddWalletActivity::class.java).start(context)
        }
    }

    override fun lazyInitListingViewModel(): ListingViewModel<AddWalletVo> {
        return ViewModelProvider(this).get(AddWalletViewModel::class.java)
    }

    override fun lazyInitListingViewAdapter(): ListingViewAdapter<AddWalletVo> {
        return ViewAdapter(onItemClick = { accountType ->
            AddWalletDialog.newInstance(accountType).show()
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.wallet_account_title_add_wallet)

        getRecyclerView().addItemDecoration(
            RecyclerViewItemDividers(
                top = DensityUtility.dp2px(this, 10),
                bottom = 0,
                onlyShowLastBottom = true
            )
        )

        getListingHandler().init()
        getListingViewModel().execute()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CREATE_IMPORT -> {
                if (resultCode == Activity.RESULT_OK) {
                    finish()
                }
            }
        }
    }

    class ViewHolder(
        view: View,
        private val onItemClick: (Int) -> Unit
    ) : BaseViewHolder<AddWalletVo>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: AddWalletVo?) {
            itemData?.let {
                itemView.ivLogo.setImageResource(it.logoId)
                itemView.tvCoinName.text = it.coinName
                itemView.tvAccountType.text = it.chainName
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: AddWalletVo?) {
            itemData?.let {
                onItemClick.invoke(it.accountType)
            }
        }
    }

    class ViewAdapter(
        private val onItemClick: (Int) -> Unit
    ) : ListingViewAdapter<AddWalletVo>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<AddWalletVo> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_add_wallet,
                    parent,
                    false
                ), onItemClick
            )
        }
    }
}

data class AddWalletVo(
    @AccountType val accountType: Int,
    @DrawableRes val logoId: Int,
    val coinName: String,   //币名
    val chainName: String   //链名
)

class AddWalletViewModel : ListingViewModel<AddWalletVo>() {

    override suspend fun loadData(vararg params: Any): List<AddWalletVo> {
        return arrayListOf(
            AddWalletVo(
                AccountType.VIOLAS,
                R.drawable.ic_violas_big,
                getViolasCoinType().coinName(),
                getViolasCoinType().chainName()
            ),
            AddWalletVo(
                AccountType.BTC,
                R.drawable.ic_bitcoin_big,
                getBitcoinCoinType().coinName(),
                getBitcoinCoinType().chainName()
            ),
            AddWalletVo(
                AccountType.LIBRA,
                R.drawable.ic_libra_big,
                getDiemCoinType().coinName(),
                getDiemCoinType().chainName()
            )
        )
    }

    override fun checkNetworkBeforeExecute(): Boolean {
        return false
    }
}