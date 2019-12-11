package com.violas.wallet.event

/**
 * Created by elephant on 2019-12-11 16:43.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 撤销交易订单事件，该事件只在订单详情中发出，在未完成订单页面中订阅处理
 */
class RevokeDexOrderEvent(val orderId: String)