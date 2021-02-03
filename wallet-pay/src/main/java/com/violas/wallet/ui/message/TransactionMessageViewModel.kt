package com.violas.wallet.ui.message

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.message.TransactionMessageDTO
import com.violas.wallet.viewModel.MessageViewModel

/**
 * Created by elephant on 12/28/20 3:33 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class TransactionMessageViewModel(
    private val address: String
) : PagingViewModel<TransactionMessageDTO>() {

    private val messageService by lazy {
        DataRepository.getMessageService()
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionMessageDTO>, Any?) -> Unit
    ) {
        if (pageNumber == 1) {
            MessageViewModel.getInstance().syncUnreadMsgNum()
        }

        val messages = messageService.getTransactionMessages(
            address,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(messages, null)
    }

    suspend fun getTransactionMsgDetails(
        message: TransactionMessageDTO
    ) =
        messageService.getTransactionMsgDetails(address, message.txnId)
}