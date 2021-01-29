package com.violas.wallet.ui.message

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.message.SystemMessageDTO

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
        val messages = messageService.getSystemMessages(
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(messages, null)
    }
}