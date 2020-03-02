package com.violas.wallet.biz.applysso

import com.violas.wallet.biz.applysso.handler.ApplyHandle
import java.util.*

class ApplyEngine(
    handleList: LinkedList<ApplyHandle>
) {
    fun execSSOApply(status: Int? = SSOApplyTokenHandler.None): Boolean {
        return false
    }
}