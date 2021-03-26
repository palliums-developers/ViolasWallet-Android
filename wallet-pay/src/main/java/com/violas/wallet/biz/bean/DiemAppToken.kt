package com.violas.wallet.biz.bean

import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.ui.main.market.bean.StableTokenVo

/**
 * Created by elephant on 3/25/21 11:10 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
data class DiemAppToken(
    val currency: DiemCurrency,
    val name: String = currency.module,
    val fullName: String = name,
    val logo: String = ""
) {
    companion object {
        fun convert(asset: MappingCoinPairDTO.Assets): DiemAppToken {
            return DiemAppToken(
                currency = DiemCurrency(
                    asset.module,
                    asset.name,
                    asset.address
                ),
                name = asset.displayName,
                logo = asset.logo
            )
        }

        fun convert(token: TokenDo): DiemAppToken {
            return DiemAppToken(
                currency = DiemCurrency(
                    token.module,
                    token.name,
                    token.address
                ),
                name = token.assetsName,
                logo = token.logo
            )
        }

        fun convert(token: StableTokenVo): DiemAppToken {
            return DiemAppToken(
                currency = DiemCurrency(
                    token.module,
                    token.name,
                    token.address
                ),
                name = token.displayName,
                logo = token.logo
            )
        }
    }
}