package com.violas.wallet.ui.main.quotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.palliums.utils.CommonViewHolder
import com.violas.wallet.R
import com.violas.wallet.ui.main.quotes.bean.IOrder
import com.violas.wallet.ui.main.quotes.bean.IOrderType
import kotlinx.android.synthetic.main.item_quotes_deepness.view.*

class AllOrderAdapter :
    ListAdapter<IOrder, CommonViewHolder>(diffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder {
        return CommonViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_quotes_deepness,
                parent,
                false
            )
        )
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
        holder.itemView.tvCoinNumber.text = item.amount()
        holder.itemView.tvCoinPrice.text = item.price()
    }
}

val diffUtil = object : DiffUtil.ItemCallback<IOrder>() {
    override fun areContentsTheSame(oldItem: IOrder, newItem: IOrder): Boolean {
        return oldItem.version() > newItem.version()
    }

    override fun areItemsTheSame(oldItem: IOrder, newItem: IOrder): Boolean {
        // TODO 添加状态
        return oldItem.version() == newItem.version()
    }

}