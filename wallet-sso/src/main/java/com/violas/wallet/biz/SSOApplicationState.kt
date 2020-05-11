package com.violas.wallet.biz

import androidx.annotation.IntDef

/**
 * Created by elephant on 2020/4/30 16:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: SSO申请状态（负数为失败情况）
 */

@IntDef(
    SSOApplicationState.ISSUER_APPLYING,
    SSOApplicationState.GOVERNOR_APPROVED,
    SSOApplicationState.CHAIRMAN_APPROVED,
    SSOApplicationState.GOVERNOR_TRANSFERRED,
    SSOApplicationState.ISSUER_PUBLISHED,
    SSOApplicationState.GOVERNOR_MINTED,
    SSOApplicationState.AUDIT_TIMEOUT,
    SSOApplicationState.GOVERNOR_UNAPPROVED,
    SSOApplicationState.CHAIRMAN_UNAPPROVED
)
annotation class SSOApplicationState {
    companion object {
        // 发行商申请发币中；
        const val ISSUER_APPLYING = 0

        // 州长已审核通过，并申请铸币权；
        const val GOVERNOR_APPROVED = 1

        // 董事长已发布新稳定币，并指定铸币权给州长；
        const val CHAIRMAN_APPROVED = 2

        // 州长已给发行商转平台币，并通知；
        const val GOVERNOR_TRANSFERRED = 3

        // 发行商已publish合约，请求铸币中；
        const val ISSUER_PUBLISHED = 4

        // 州长已铸币给发行商；
        const val GOVERNOR_MINTED = 5

        // 审核超时
        const val AUDIT_TIMEOUT = -1

        // 州长审核未通过；
        const val GOVERNOR_UNAPPROVED = -2

        // 董事长审核未通过。
        const val CHAIRMAN_UNAPPROVED = -3
    }
}