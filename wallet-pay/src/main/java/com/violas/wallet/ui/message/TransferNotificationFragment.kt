package com.violas.wallet.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.repository.http.message.NotificationMsgDTO

/**
 * Created by elephant on 12/28/20 2:54 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 转账通知Tab页
 */
class TransferNotificationFragment : BasePagingFragment<NotificationMsgDTO>() {

    override fun lazyInitPagingViewModel(): PagingViewModel<NotificationMsgDTO> {
        return ViewModelProvider(this).get(TransferNotificationViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<NotificationMsgDTO> {
        return ViewAdapter { msg, index ->

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            R.string.tips_msg_list_empty
        )
        getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_LOADING)
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)

        getPagingHandler().init()
        getPagingHandler().start()
    }

    class ViewAdapter(
        private val itemClickCallback: (NotificationMsgDTO, Int) -> Unit
    ) : PagingViewAdapter<NotificationMsgDTO>() {

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_home_bank_product, parent, false
                ),
                itemClickCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val itemClickCallback: (NotificationMsgDTO, Int) -> Unit
    ) : BaseViewHolder<NotificationMsgDTO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: NotificationMsgDTO?) {
            itemData?.let {

            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: NotificationMsgDTO?) {
            itemData?.let {
                itemClickCallback.invoke(it, itemPosition)
            }
        }
    }
}