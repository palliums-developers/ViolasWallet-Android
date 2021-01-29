package com.violas.wallet.event

/**
 * Created by elephant on 2020/6/12 15:58.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class SwitchHomePageEvent(val homePageType: HomePageType)

enum class HomePageType {
    Wallet, Market, Bank, Me
}