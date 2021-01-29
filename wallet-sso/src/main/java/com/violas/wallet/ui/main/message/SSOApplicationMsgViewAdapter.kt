package com.violas.wallet.ui.main.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.utils.formatDate
import com.palliums.utils.getColor
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.SSOApplicationState
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
    private val mItemCallback: (SSOApplicationMsgVO) -> Unit
) : PagingViewAdapter<SSOApplicationMsgVO>() {

    private val mDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)

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
            itemView.tvTitle.text =
                if (it.applicationStatus <= SSOApplicationState.GOVERNOR_TRANSFERRED) {
                    getString(R.string.title_sso_msg_issuing_token, it.applicantIdName)
                } else {
                    getString(R.string.title_sso_msg_mint_token, it.applicantIdName)
                }

            when (it.applicationStatus) {
                SSOApplicationState.ISSUER_APPLYING -> {
                    itemView.tvStatus.visibility = View.GONE
                    itemView.vUnread.visibility = if (it.msgRead) View.GONE else View.VISIBLE
                }

                SSOApplicationState.GOVERNOR_APPROVED -> {
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.visibility = View.VISIBLE
                    itemView.tvStatus.text = getString(R.string.state_applying_mintable)
                    itemView.tvStatus.setTextColor(getColor(R.color.color_FAA030))
                }

                SSOApplicationState.CHAIRMAN_APPROVED,
                SSOApplicationState.GOVERNOR_TRANSFERRED -> {
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.visibility = View.VISIBLE
                    itemView.tvStatus.text = getString(R.string.state_given_mintable)
                    itemView.tvStatus.setTextColor(getColor(R.color.color_00D1AF))
                }

                SSOApplicationState.ISSUER_PUBLISHED -> {
                    itemView.tvStatus.visibility = View.GONE
                    itemView.vUnread.visibility = if (it.msgRead) View.GONE else View.VISIBLE
                }

                SSOApplicationState.GOVERNOR_MINTED -> {
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.visibility = View.VISIBLE
                    itemView.tvStatus.text = getString(R.string.state_completed)
                    itemView.tvStatus.setTextColor(getColor(R.color.def_text_title))
                }

                SSOApplicationState.GOVERNOR_UNAPPROVED -> {
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.visibility = View.VISIBLE
                    itemView.tvStatus.text = getString(R.string.state_governor_unapproved)
                    itemView.tvStatus.setTextColor(getColor(R.color.def_text_title))
                }

                SSOApplicationState.CHAIRMAN_UNAPPROVED -> {
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.visibility = View.VISIBLE
                    itemView.tvStatus.text = getString(R.string.state_chairman_unapproved)
                    itemView.tvStatus.setTextColor(getColor(R.color.color_F55753))
                }

                else -> {
                    itemView.vUnread.visibility = View.GONE
                    itemView.tvStatus.visibility = View.VISIBLE
                    itemView.tvStatus.text = getString(R.string.state_closed)
                    itemView.tvStatus.setTextColor(getColor(R.color.def_text_title))
                }
            }
        }
    }

    override fun onViewClick(view: View, itemPosition: Int, itemData: SSOApplicationMsgVO?) {
        itemData?.let { mItemCallback.invoke(it) }
    }
}