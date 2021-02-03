package com.violas.wallet.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.extensions.getShowErrorMessage
import com.palliums.paging.PagingViewAdapter
import com.palliums.paging.PagingViewModel
import com.palliums.utils.formatDate
import com.palliums.utils.getDrawableByAttrId
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BasePagingFragment
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.event.ClearUnreadMessagesEvent
import com.violas.wallet.event.ReadOneTransactionMsgEvent
import com.violas.wallet.repository.http.message.TransactionMessageDTO
import com.violas.wallet.ui.transactionDetails.TransactionDetailsActivity
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType
import kotlinx.android.synthetic.main.item_transaction_message.view.*
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
 * desc: 转账通知Tab页
 */
class TransactionMessageFragment : BasePagingFragment<TransactionMessageDTO>() {

    override fun lazyInitPagingViewModel(): PagingViewModel<TransactionMessageDTO> {
        return ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return modelClass
                        .getConstructor(String::class.java)
                        .newInstance(address)
                }
            }
        ).get(TransactionMessageViewModel::class.java)
    }

    override fun lazyInitPagingViewAdapter(): PagingViewAdapter<TransactionMessageDTO> {
        return ViewAdapter { msg, index ->
            onItemClick(msg, index)
        }
    }

    private lateinit var address: String

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

        launch {
            val initResult = initAddress()
            getPagingHandler().init()
            if (initResult) {
                EventBus.getDefault().register(this@TransactionMessageFragment)
                getPagingHandler().start()
            } else {
                getStatusLayout()?.showStatus(IStatusLayout.Status.STATUS_EMPTY)
            }
        }
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

    private suspend fun initAddress() = withContext(Dispatchers.IO) {
        val violasAccount =
            AccountManager().getIdentityByCoinType(CoinTypes.Violas.coinType())
                ?: return@withContext false

        address = violasAccount.address
        return@withContext true
    }

    private fun getViewModel(): TransactionMessageViewModel {
        return getPagingViewModel() as TransactionMessageViewModel
    }

    private fun onItemClick(message: TransactionMessageDTO, index: Int) {
        launch(Dispatchers.IO) {
            showProgress()
            try {
                val msgDetails = getViewModel().getTransactionMsgDetails(message)

                val transactionState =
                    if (msgDetails.status.equals("Executed", true))
                        TransactionState.SUCCESS
                    else
                        TransactionState.FAILURE
                val transactionType =
                    if (msgDetails.type.equals("ADD_CURRENCY_TO_ACCOUNT", true)
                        || msgDetails.type.equals("0", true)
                    ) {
                        TransactionType.ADD_CURRENCY
                    } else if (msgDetails.sender != address && msgDetails.receiver == address) {
                        TransactionType.COLLECTION
                    } else if (msgDetails.sender == address && !msgDetails.receiver.isNullOrBlank()) {
                        TransactionType.TRANSFER
                    } else {
                        TransactionType.OTHER
                    }
                val transactionRecordVO = TransactionRecordVO(
                    id = 0,
                    coinType = CoinTypes.Violas,
                    transactionState = transactionState,
                    transactionType = transactionType,
                    time = msgDetails.expirationTime,
                    fromAddress = msgDetails.sender,
                    toAddress = msgDetails.receiver,
                    amount = msgDetails.amount,
                    tokenId = msgDetails.currency,
                    tokenDisplayName = msgDetails.currency,
                    gas = msgDetails.gas,
                    gasTokenId = msgDetails.gasCurrency,
                    gasTokenDisplayName = msgDetails.gasCurrency,
                    transactionId = msgDetails.txnId,
                    url = BaseBrowserUrl.getViolasBrowserUrl(msgDetails.txnId)
                )

                dismissProgress()
                withContext(Dispatchers.Main) {
                    TransactionDetailsActivity.start(requireContext(), transactionRecordVO)
                    if (message.markAsRead()) {
                        getPagingViewAdapter().notifyItemChanged(index)
                        EventBus.getDefault().post(ReadOneTransactionMsgEvent())
                    }
                }
            } catch (e: Exception) {
                dismissProgress()
                showToast(e.getShowErrorMessage(true))
            }
        }
    }

    class ViewAdapter(
        private val itemClickCallback: (TransactionMessageDTO, Int) -> Unit
    ) : PagingViewAdapter<TransactionMessageDTO>() {

        private val simpleDateFormat = SimpleDateFormat("MM.dd HH:mm", Locale.ENGLISH)

        override fun onCreateViewHolderSupport(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<out Any> {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_transaction_message, parent, false
                ),
                simpleDateFormat,
                itemClickCallback
            )
        }
    }

    class ViewHolder(
        view: View,
        private val simpleDateFormat: SimpleDateFormat,
        private val itemClickCallback: (TransactionMessageDTO, Int) -> Unit
    ) : BaseViewHolder<TransactionMessageDTO>(view) {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onViewBind(itemPosition: Int, itemData: TransactionMessageDTO?) {
            itemData?.let {
                itemView.tvTitle.text = it.title
                itemView.tvDesc.text = it.body
                itemView.tvTime.text = formatDate(it.time, simpleDateFormat)
                itemView.vStatus.background = getDrawableByAttrId(
                    if (it.txnStatus.equals("Executed", true))
                        R.attr.iconRecordStateSucceeded
                    else
                        R.attr.iconRecordStateFailed,
                    itemView.context
                )
                itemView.setBackgroundResource(
                    if (it.read())
                        R.drawable.bg_menu_item
                    else
                        R.drawable.sel_bg_msg_unread
                )
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: TransactionMessageDTO?) {
            itemData?.let {
                itemClickCallback.invoke(it, itemPosition)
            }
        }
    }
}