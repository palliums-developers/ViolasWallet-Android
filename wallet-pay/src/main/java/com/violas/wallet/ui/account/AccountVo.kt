package com.violas.wallet.ui.account

import com.palliums.widget.groupList.GroupListLayout
import com.violas.wallet.repository.database.entity.AccountDO

/**
 * Created by elephant on 2019-10-24 17:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 钱包账户的View Object
 */
class AccountVo(
    val accountDO: AccountDO, var selected: Boolean = false
) : GroupListLayout.ItemData {

    private var groupName: String? = null

    override fun getGroupName(): String? {
        return this.groupName
    }

    override fun setGroupName(groupName: String) {
        this.groupName = groupName
    }

    override fun compareTo(other: String): Int {
        return 0
    }
}