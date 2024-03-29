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
import com.palliums.widget.dividers.RecycleViewItemDividers
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseListingActivity
import com.violas.wallet.common.Vm
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

    private val mViewModel by lazy {
        ViewModelProvider(this).get(AddWalletViewModel::class.java)
    }

    private val mViewAdapter by lazy {
        AddWalletAdapter(onItemClick = { accountType ->
            AddWalletDialog.newInstance(accountType).show()
        })
    }

    override fun getViewModel(): ListingViewModel<AddWalletVo> {
        return mViewModel
    }

    override fun getViewAdapter(): ListingViewAdapter<AddWalletVo> {
        return mViewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.add_wallet_title)

        getRecyclerView().addItemDecoration(
            RecycleViewItemDividers(
                top = DensityUtility.dp2px(this, 10),
                bottom = 0,
                showFirstTop = true,
                onlyShowLastBottom = true
            )
        )

        mViewModel.execute()
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
}

data class AddWalletVo(
    @AccountType val accountType: Int,
    @DrawableRes val logoId: Int,
    val coinName: String,   //币名
    val chainName: String   //链名
)

class AddWalletViewHolder(view: View, private val onItemClick: (Int) -> Unit) :
    BaseViewHolder<AddWalletVo>(view) {

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

class AddWalletAdapter(private val onItemClick: (Int) -> Unit) : ListingViewAdapter<AddWalletVo>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<AddWalletVo> {
        return AddWalletViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_add_wallet,
                parent,
                false
            ), onItemClick
        )
    }
}

class AddWalletViewModel : ListingViewModel<AddWalletVo>() {

    override suspend fun loadData(vararg params: Any): List<AddWalletVo> {
        return arrayListOf(
            AddWalletVo(
                AccountType.VIOLAS,
                R.mipmap.ic_label_violas,
                CoinTypes.Violas.coinName(),
                CoinTypes.Violas.fullName()
            ),
            AddWalletVo(
                AccountType.BTC,
                R.mipmap.ic_label_btc,
                if (Vm.TestNet) CoinTypes.BitcoinTest.coinName() else CoinTypes.Bitcoin.coinName(),
                if (Vm.TestNet) CoinTypes.BitcoinTest.fullName() else CoinTypes.Bitcoin.fullName()
            ),
            AddWalletVo(
                AccountType.LIBRA,
                R.mipmap.ic_label_libra,
                CoinTypes.Libra.coinName(),
                CoinTypes.Libra.fullName()
            )
        )
    }

    override fun checkNetworkBeforeExecute(): Boolean {
        return false
    }
}