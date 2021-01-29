package com.violas.wallet.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.utils.openBrowser
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.repository.http.message.SystemMessageDTO
import com.violas.wallet.ui.web.WebCommonActivity
import kotlinx.android.synthetic.main.item_system_message.view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 12/28/20 2:54 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 系统通知Tab页
 */
class SystemMessageFragment : BasePagingFragment<SystemMessageDTO>() {

    override fun lazyInitPagingViewModel(): PagingViewModel<SystemMessageDTO> {
        return ViewModelProvider(this).get(SystemMessageViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<SystemMessageDTO> {
        return ViewAdapter { msg, index ->
            if (msg.url.isBlank()) {
                showToast(R.string.msg_center_tips_system_msg_url_empty)
            } else {
                //if (!openBrowser(requireContext(), msg.url)) {
                WebCommonActivity.start(requireContext(), msg.url)
                //}
                launch {
                    delay(1000)
                    msg.readStatus = 1
                    getPagingViewAdapter().notifyItemChanged(index)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            R.string.msg_center_tips_messages_empty
        )
        getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_LOADING)
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        getPagingHandler().init()
        getPagingHandler().start()
    }

    class ViewAdapter(
        private val itemClickCallback: (SystemMessageDTO, Int) -> Unit
    ) : PagingViewAdapter<SystemMessageDTO>() {

        private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm", Locale.ENGLISH)

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_system_message, parent, false
                ),
                simpleDateFormat,
                itemClickCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val simpleDateFormat: SimpleDateFormat,
        private val itemClickCallback: (SystemMessageDTO, Int) -> Unit
    ) : BaseViewHolder<SystemMessageDTO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: SystemMessageDTO?) {
            itemData?.let {
                itemView.tvTitle.text = it.title
                itemView.tvDesc.text = it.body
                itemView.tvTime.text = formatDate(it.time, simpleDateFormat)
                itemView.setBackgroundResource(
                    if (it.readStatus == 1)
                        R.drawable.bg_menu_item
                    else
                        R.drawable.sel_bg_msg_unread
                )
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: SystemMessageDTO?) {
            itemData?.let {
                itemClickCallback.invoke(it, itemPosition)
            }
        }
    }
}