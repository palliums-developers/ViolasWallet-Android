package com.violas.wallet.ui.main.quotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.palliums.utils.CommonViewHolder
import com.violas.wallet.R
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
        holder.itemView.ivOrderStatus.setImageDrawable(null)
        holder.itemView.tvCoinNumber.text = ""
        holder.itemView.tvCoinPrice.text = ""
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