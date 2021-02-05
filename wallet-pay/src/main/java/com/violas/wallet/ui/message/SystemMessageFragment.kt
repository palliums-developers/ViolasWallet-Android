package com.violas.wallet.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.extensions.getShowErrorMessage
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.event.ClearUnreadMessagesEvent
import com.violas.wallet.event.ReadOneSystemMsgEvent
import com.violas.wallet.repository.http.message.SystemMessageDTO
import com.violas.wallet.ui.message.details.SystemMsgDetailsActivity
import com.violas.wallet.viewModel.MessageViewModel
import kotlinx.android.synthetic.main.item_system_message.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
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
            onItemClick(msg, index)
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
        EventBus.getDefault().register(this)
        getPagingHandler().init()
        getPagingHandler().start()
    }

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
    }

    @Subscribe
    fun onClearUnreadMessagesEvent(event: ClearUnreadMessagesEvent) {
        launch(Dispatchers.IO) {
            var needNotify = false
            getPagingViewAdapter().currentList?.forEach {
                if (it.markAsRead()) {
                    needNotify = true
                }
            }
            withContext(Dispatchers.Main) {
                if (needNotify) {
                    getPagingViewAdapter().notifyDataSetChanged()
                }
            }
        }
    }

    private fun getViewModel(): SystemMessageViewModel {
        return getPagingViewModel() as SystemMessageViewModel
    }

    private fun onItemClick(message: SystemMessageDTO, index: Int) {
        launch(Dispatchers.IO) {
            showProgress()
            try {
                val token = MessageViewModel.getInstance().getFirebaseToken()
                val msgDetails = getViewModel().getSystemMsgDetails(message, token!!)

                dismissProgress()
                withContext(Dispatchers.Main) {
                    SystemMsgDetailsActivity.start(requireContext(), msgDetails)
                    if (message.markAsRead()) {
                        getPagingViewAdapter().notifyItemChanged(index)
                        EventBus.getDefault().post(ReadOneSystemMsgEvent())
                    }
                }
            } catch (e: Exception) {
                dismissProgress()
                showToast(e.getShowErrorMessage(true))
            }
        }
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
                    if (it.read())
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