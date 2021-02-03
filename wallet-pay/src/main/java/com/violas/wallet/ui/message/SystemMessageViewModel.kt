package com.violas.wallet.ui.message

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.message.SystemMessageDTO
import com.violas.wallet.viewModel.MessageViewModel
import com.violas.wallet.viewModel.WalletAppViewModel

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
    private val accountManager by lazy {
        WalletAppViewModel.getViewModelInstance().mAccountManager
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

        val messages = messageService.getSystemMessages(
            accountManager.getAppToken(),
            pageSize,
            (pageNumber - 1) * pageSize
        )
        onSuccess.invoke(messages, null)
    }

    suspend fun getSystemMsgDetails(
        message: SystemMessageDTO
    ) =
        messageService.getTransactionMsgDetails(message.id)
}