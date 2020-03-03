package com.violas.wallet.ui.main.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.utils.formatDate
import com.palliums.utils.getColor
import com.palliums.utils.getString
import com.violas.wallet.R
import kotlinx.android.synthetic.main.item_sso_application_msg.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2020/3/2 21:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SSOApplicationMsgViewAdapter(
    retryCallback: () -> Unit,
    private val mItemCallback: (SSOApplicationMsgVO) -> Unit
) : PagingViewAdapter<SSOApplicationMsgVO>(retryCallback, DiffCallback()) {

    private val mDateFormat = SimpleDateFormat("yy.MM.dd HH:mm", Locale.ENGLISH)

    override fun onCreateViewHolderSupport(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<out Any> {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_sso_application_msg,
                parent,
                false
            ),
            mDateFormat,
            mItemCallback
        )
    }

    override fun showNoMoreView(): Boolean {
        return false
    }
}

private class ViewHolder(
    view: View,
    private val mDateFormat: SimpleDateFormat,
    private val mItemCallback: (SSOApplicationMsgVO) -> Unit
) : BaseViewHolder<SSOApplicationMsgVO>(view) {

    init {
        itemView.setOnClickListener(this)
    }

    override fun onViewBind(itemPosition: Int, itemData: SSOApplicationMsgVO?) {
        itemData?.let {
            itemView.tvTime.text = formatDate(it.applicationDate, mDateFormat)
            when (it.applicationStatus) {
                0 -> {
                    itemView.tvTitle.text =
                        getString(R.string.title_sso_msg_issuing_token, it.applicantIdName)
                    itemView.tvStatus.visibility = View.GONE
                    itemView.vUnread.visibility = if (it.msgUnread) View.VISIBLE else View.GONE
                }

                1 -> {
                    itemView.tvTitle.text =
                        getString(R.string.title_sso_msg_issuing_token, it.applicantIdName)
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.text = getString(R.string.state_passed)
                    itemView.tvStatus.setTextColor(getColor(R.color.def_text_title))
                    itemView.tvStatus.visibility = View.VISIBLE
                }

                2 -> {
                    itemView.tvTitle.text =
                        getString(R.string.title_sso_msg_issuing_token, it.applicantIdName)
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.text = getString(R.string.state_rejected)
                    itemView.tvStatus.setTextColor(getColor(R.color.color_E54040))
                    itemView.tvStatus.visibility = View.VISIBLE
                }

                3 -> {
                    itemView.tvTitle.text = getString(R.string.title_sso_msg_mint_token, it.applicantIdName)
                    itemView.tvStatus.visibility = View.GONE
                    itemView.vUnread.visibility = if (it.msgUnread) View.VISIBLE else View.GONE
                }

                else -> {
                    itemView.tvTitle.text = getString(R.string.title_sso_msg_mint_token, it.applicantIdName)
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.text = getString(R.string.state_completed)
                    itemView.tvStatus.setTextColor(getColor(R.color.def_text_title))
                    itemView.tvStatus.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: SSOApplicationMsgVO?) {
        itemData?.let { mItemCallback.invoke(it) }
    }
}

private class DiffCallback : DiffUtil.ItemCallback<SSOApplicationMsgVO>() {
    override fun areItemsTheSame(
        oldItem: SSOApplicationMsgVO,
        newItem: SSOApplicationMsgVO
    ): Boolean {
        return oldItem.applicationId == newItem.applicationId
    }

    override fun areContentsTheSame(
        oldItem: SSOApplicationMsgVO,
        newItem: SSOApplicationMsgVO
    ): Boolean {
        return oldItem == newItem
    }
}