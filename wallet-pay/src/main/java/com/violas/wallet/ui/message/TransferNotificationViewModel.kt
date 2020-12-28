package com.violas.wallet.ui.message

import com.palliums.paging.PagingViewModel
import com.violas.wallet.repository.http.message.NotificationMsgDTO

/**
 * Created by elephant on 12/28/20 3:33 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class TransferNotificationViewModel : PagingViewModel<NotificationMsgDTO>() {

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<NotificationMsgDTO>, Any?) -> Unit
    ) {
        // TODO
        onSuccess.invoke(emptyList(), null)
    }
}