package com.violas.wallet.event

import com.violas.wallet.repository.http.governor.GovernorInfoDTO

/**
 * Created by elephant on 2020/3/6 21:25.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class UpdateGovernorInfoEvent(
    val governorInfo: GovernorInfoDTO
)