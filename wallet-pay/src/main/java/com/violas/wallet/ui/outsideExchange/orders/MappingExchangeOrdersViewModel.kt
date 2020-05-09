package com.violas.wallet.ui.outsideExchange.orders

import androidx.annotation.Keep
import com.palliums.paging.PagingViewModel
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.utils.convertAmountToDisplayUnit
import com.violas.wallet.utils.validationBTCAddress
import java.util.*

/**
 * Created by elephant on 2020-02-18 12:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingExchangeOrdersViewModel(
    private val walletAddress: String,
    private val walletType: Int
) : PagingViewModel<MappingExchangeOrderVO>() {

    private val mappingExchangeService by lazy {
        DataRepository.getMappingExchangeService()
    }

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<MappingExchangeOrderVO>, Any?) -> Unit
    ) {
        val response =
            mappingExchangeService.getMappingExchangeOrders(
                walletAddress,
                walletType,
                pageSize,
                (pageNumber - 1) * pageSize
            )

        if (response.data == null || response.data!!.list.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.list!!.mapIndexed { index, dto ->

            val coinType = if (!dto.coin.isNullOrEmpty()) {
                when (dto.coin.toLowerCase(Locale.ENGLISH)) {
                    "libra" -> {
                        CoinTypes.Libra
                    }

                    "btc" -> {
                        if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin
                    }

                    else -> {
                        CoinTypes.Violas
                    }
                }
            } else if (validationBTCAddress(dto.address)) {
                if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin
            } else {
                CoinTypes.Libra
            }
            val amountInfo = convertAmountToDisplayUnit(dto.amount, coinType)

            MappingExchangeOrderVO(
                id = (pageNumber - 1) * pageSize + index,
                time = dto.date * 1000,
                status = dto.status,
                amount = "${amountInfo.first} ${dto.coin ?: ""}",
                address = dto.address,
                coinName = if (dto.coin.isNullOrEmpty()) "" else dto.coin
            )
        }
        onSuccess.invoke(list, null)
    }
}