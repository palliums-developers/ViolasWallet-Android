package com.violas.wallet.ui.main.quotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.palliums.utils.CommonViewHolder
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_quotes_entrust.view.*

class MeOrderAdapter :
    ListAdapter<IOrder, CommonViewHolder>(diffUtil) {
    private val mMaxCount = 3

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
        holder.itemView.ivOrderStatus.setImageDrawable(null)
        holder.itemView.tvFromCoin.text = ""
        holder.itemView.tvToCoin.text = ""
        holder.itemView.tvCoinNumber.text = ""
        holder.itemView.tvCoinPrice.text = ""
        holder.itemView.tvYear.text = ""
        holder.itemView.tvTime.text = ""
    }
}