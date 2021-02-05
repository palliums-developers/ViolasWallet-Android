package com.violas.wallet.ui.message

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.message.SystemMessageDTO
import com.violas.wallet.viewModel.MessageViewModel

/**
 * Created by elephant on 12/28/20 3:33 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SystemMessageViewModel : PagingViewModel<SystemMessageDTO>() {

    private val messageService by lazy {
        DataRepository.getMessageService()
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<SystemMessageDTO>, Any?) -> Unit
    ) {
        if (pageNumber == 1) {
            MessageViewModel.getInstance().syncUnreadMsgNum()
        }

        val token = MessageViewModel.getInstance().getFirebaseToken()
        if(token.isNullOrBlank()){
            onSuccess.invoke(emptyList(), null)
            return
        }

        val messages = messageService.getSystemMessages(
            token,
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(messages, null)
    }

    suspend fun getSystemMsgDetails(
        message: SystemMessageDTO,
        token: String
    ) =
        messageService.getSystemMsgDetails(message.id, token)
}