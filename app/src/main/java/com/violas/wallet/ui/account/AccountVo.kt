package com.violas.wallet.ui.account

import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.widget.GroupListLayout

/**
 * Created by elephant on 2019-10-24 17:54.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 钱包账户的View Object
 */
class AccountVo(val accountDO: AccountDO) : GroupListLayout.ItemData {

    private var groupName: String? = null

    override fun getGroupName(): String? {
        return this.groupName
    }

    override fun setGroupName(groupName: String) {
        this.groupName = groupName
    }
}