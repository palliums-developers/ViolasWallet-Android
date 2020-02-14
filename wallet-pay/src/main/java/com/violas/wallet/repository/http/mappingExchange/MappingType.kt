package com.violas.wallet.repository.http.mappingExchange

/**
 * Created by elephant on 2020-02-14 11:52.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
enum class MappingType(val typeName:String) {
    BTCToVbtc("btc"),
    VbtcToBTC("vbtc"),
    LibraToVlibra("libra"),
    VlibraToLibra("vlibra"),
}