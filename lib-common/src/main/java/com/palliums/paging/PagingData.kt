package com.palliums.paging

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.palliums.net.LoadState

/**
 * Created by elephant on 2019-08-13 11:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: PagingData
 *
 * Data class that is necessary for a UI to show a listing and interact w/ the rest of the system
 */
data class PagingData<VO>(

    // the LiveData of paged lists for the UI to observe
    val pagedList: LiveData<PagedList<VO>>,

    // represents the refresh status to show to the user.
    // Separate from loadMoreState, this value is importtantly only when refresh is requested.
    val refreshState: LiveData<LoadState>,

    // represents the load more status to show to the user.
    val loadMoreState: LiveData<LoadState>,

    // tips message
    val tipsMessage: LiveData<String>,

    // refreshes the whole data and it from scratch.
    val refresh: () -> Unit,

    // retries any failed requests.
    val retry: () -> Unit
)