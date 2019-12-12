package com.violas.wallet.ui.main.quotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ListAdapter
import com.palliums.utils.CommonViewHolder
import com.violas.wallet.R
import com.violas.wallet.ui.main.quotes.bean.IOrder
import com.violas.wallet.ui.main.quotes.bean.IOrderType
import kotlinx.android.synthetic.main.item_quotes_entrust.view.*
import java.text.SimpleDateFormat

class MeOrderAdapter :
    ListAdapter<IOrder, CommonViewHolder>(diffUtil) {
    private val mMaxCount = 3

    private val mYearSimpleDateFormat = SimpleDateFormat("MM/dd")
    private val mTimeSimpleDateFormat = SimpleDateFormat("HH:mm:ss")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder {
        return CommonViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_quotes_entrust,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        val itemCount = super.getItemCount()
        return if (itemCount > mMaxCount) {
            mMaxCount
        } else {
            itemCount
        }
    }

    override fun onBindViewHolder(holder: CommonViewHolder, position: Int) {
        val item = getItem(position)
        val drawableRes = when (item.type()) {
            IOrderType.SELLS -> {
                R.drawable.icon_quotes_sell
            }
            IOrderType.BUY -> {
                R.drawable.icon_quotes_buy
            }
        }
        holder.itemView.ivOrderStatus.setImageDrawable(
            ResourcesCompat.getDrawable(
                holder.itemView.context.resources,
                drawableRes,
                null
            )
        )
        holder.itemView.tvFromCoin.text = "${item.tokenGiveSymbol()}/"
        holder.itemView.tvToCoin.text = item.tokenGetSymbol()
        holder.itemView.tvCoinNumber.text = item.amount()
        holder.itemView.tvCoinPrice.text = item.price()
        holder.itemView.tvYear.text = mYearSimpleDateFormat.format(item.date())
        holder.itemView.tvTime.text = mTimeSimpleDateFormat.format(item.date())
    }
}