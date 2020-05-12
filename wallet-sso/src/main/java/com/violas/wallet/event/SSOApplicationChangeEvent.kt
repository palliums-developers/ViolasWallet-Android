package com.violas.wallet.event

import com.violas.wallet.repository.http.issuer.ApplyForSSOSummaryDTO

/**
 * Created by elephant on 2020/5/7 17:31.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SSOApplicationChangeEvent(val summary: ApplyForSSOSummaryDTO?)