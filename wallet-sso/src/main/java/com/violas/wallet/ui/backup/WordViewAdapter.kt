package com.violas.wallet.ui.backup

import android.util.Log
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
    val index: Int,
    val clickable: Boolean,
    var confirmed: Boolean = false
)

class WordViewHolder(
    view: View,
    private val confirmed: Boolean = false,
    private val onItemClick: ((Int, WordVO) -> Unit)? = null
) : BaseViewHolder<WordVO>(view) {

    init {
        if (onItemClick != null) {
            itemView.setOnClickListener(this)
        }
    }

    override fun onViewBind(itemIndex: Int, itemData: WordVO?) {
        itemData?.let {
            itemView.vWord.text = it.word
            when {
                confirmed -> {
                    itemView.vWord.setTextColor(getColor(R.color.white, itemView.context))
                    itemView.vWord.setBackgroundResource(R.drawable.selector_bg_mnemonic_word_comfirmed)
                }

                it.clickable -> {
                    itemView.vWord.setTextColor(getColor(R.color.color_3C3848, itemView.context))
                    itemView.vWord.setBackgroundResource(R.drawable.selector_bg_mnemonic_word)
                    itemView.vWord.isEnabled = !it.confirmed
                }

                else -> {
                    itemView.vWord.setTextColor(getColor(R.color.color_3C3848, itemView.context))
                    itemView.vWord.setBackgroundResource(R.drawable.bg_mnemonic_word_normal)
                }
            }
        }
    }

    override fun onViewClick(view: View, itemIndex: Int, itemData: WordVO?) {
        itemData?.let {
            onItemClick?.invoke(itemIndex, it)
        }
    }
}

class WordViewAdapter(
    dataList: MutableList<WordVO>,
    private val confirmed: Boolean = false, // true: 确认的助记词；
    private val onItemClick: ((Int, WordVO) -> Unit)? = null
) : ListingViewAdapter<WordVO>(dataList) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<WordVO> {
        return WordViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_mnemonic_word,
                parent,
                false
            ), confirmed, onItemClick
        )
    }
}