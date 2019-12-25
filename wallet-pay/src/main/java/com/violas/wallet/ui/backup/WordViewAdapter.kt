package com.violas.wallet.ui.backup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.utils.getColor
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_mnemonic_word.view.*

/**
 * Created by elephant on 2019-11-20 11:00.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

data class WordVO(
    val word: String,
    val index: Int
){
    var confirmed: Boolean = false
}

class WordViewHolder(
    view: View,
    private val confirmView: Boolean = false,
    private val onItemClick: ((Int, WordVO) -> Unit)? = null
) : BaseViewHolder<WordVO>(view) {

    init {
        if (onItemClick != null) {
            itemView.setOnClickListener(this)
        }
    }

    override fun onViewBind(itemPosition: Int, itemData: WordVO?) {
        itemData?.let {
            itemView.vWord.text = it.word
            when {
                onItemClick == null ->{
                    itemView.vWord.setTextColor(getColor(R.color.color_3C3848, itemView.context))
                    itemView.vWord.setBackgroundResource(R.drawable.bg_mnemonic_word_normal)
                }
                confirmView -> {
                    itemView.vWord.setTextColor(getColor(R.color.white, itemView.context))
                    itemView.vWord.setBackgroundResource(R.drawable.selector_bg_mnemonic_word_comfirmed)
                }
                it.confirmed -> {
                    itemView.vWord.setTextColor(getColor(R.color.color_3C3848_50, itemView.context))
                    itemView.vWord.setBackgroundResource(R.drawable.selector_bg_mnemonic_word)
                    itemView.vWord.isEnabled = false
                }
                else -> {
                    itemView.vWord.setTextColor(getColor(R.color.color_3C3848, itemView.context))
                    itemView.vWord.setBackgroundResource(R.drawable.selector_bg_mnemonic_word)
                    itemView.vWord.isEnabled = true
                }
            }
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: WordVO?) {
        itemData?.let {
            onItemClick?.invoke(itemPosition, it)
        }
    }
}

class WordViewAdapter(
    dataList: MutableList<WordVO>,
    private val confirmView: Boolean = false, // true: 确认的助记词视图；
    private val onItemClick: ((Int, WordVO) -> Unit)? = null
) : ListingViewAdapter<WordVO>(dataList) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<WordVO> {
        return WordViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_mnemonic_word,
                parent,
                false
            ), confirmView, onItemClick
        )
    }
}