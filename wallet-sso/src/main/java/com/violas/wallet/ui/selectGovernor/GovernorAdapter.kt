package com.violas.wallet.ui.selectGovernor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.violas.wallet.R
import com.violas.wallet.repository.http.sso.GovernorDTO
import com.violas.wallet.ui.selectCurrency.StickyHeaderAdapter
import kotlinx.android.synthetic.main.item_support_currency.view.*
import java.util.*

class GovernorAdapter(
    context: Context,
    private val contactLists: ArrayList<GovernorDTO>,
    private val onClick: (GovernorDTO) -> Unit
) : RecyclerView.Adapter<GovernorAdapter.ViewHolder>(),
    StickyHeaderAdapter<GovernorAdapter.HeaderHolder> {
    private val mInflater: LayoutInflater
    private var lastChar = '\u0000'
    private var DisplayIndex = 0

    init {
        mInflater = LayoutInflater.from(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = mInflater.inflate(R.layout.item_applyfor_governor, parent, false)
        return ViewHolder(view).apply {
            itemView.setOnClickListener { onClick.invoke(contactLists[layoutPosition]) }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (_, _,name, _, _) = contactLists[position]
        holder.itemView.tvName.text = name
    }

    override fun getItemCount(): Int {
        return contactLists.size
    }

    //=================悬浮栏=================
    override fun getHeaderId(position: Int): Long {
        //这里面的是如果当前position与之前position重复（内部判断）  则不显示悬浮标题栏  如果不一样则显示标题栏
        if (null != contactLists[position] && contactLists[position].getNameFirst() != '\u0000') {
            val ch = contactLists[position].getNameFirst()
            if (lastChar == '\u0000') {
                lastChar = ch
                return DisplayIndex.toLong()
            } else {
                if (lastChar == ch) {
                    return DisplayIndex.toLong()
                } else {
                    lastChar = ch
                    DisplayIndex++
                    return DisplayIndex.toLong()
                }

            }
        } else {
            return DisplayIndex.toLong()
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderHolder {
        val view = mInflater.inflate(R.layout.item_aoolefor_governor_head, parent, false)
        return HeaderHolder(view)
    }

    override fun onBindHeaderViewHolder(viewholder: HeaderHolder, position: Int) {
        if (contactLists[position].getNameFirst() == '\u0000') {
            viewholder.header.text = "#"
        } else {
            viewholder.header.text = contactLists[position].getNameFirst() + ""
        }
    }

    fun getPositionForSection(pinyinFirst: Char): Int {
        for (i in 0 until itemCount) {
            val firstChar = contactLists[i].getNameFirst()
            if (firstChar == pinyinFirst) {
                return i
            }
        }
        return -1
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var header: TextView

        init {
            header = itemView as TextView
        }
    }
}