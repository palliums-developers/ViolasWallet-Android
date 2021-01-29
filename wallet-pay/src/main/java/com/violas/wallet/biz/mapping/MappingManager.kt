package com.violas.wallet.biz.mapping

import com.palliums.violas.smartcontract.ViolasMultiTokenContract
import com.violas.wallet.biz.mapping.processor.BitcoinToMappingCoinProcessor
import com.violas.wallet.biz.mapping.processor.LibraToMappingCoinProcessor
import com.violas.wallet.biz.mapping.processor.ViolasToOriginalCoinProcessor
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO

/**
 * Created by elephant on 2020/8/13 16:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingManager {

    private val contract = ViolasMultiTokenContract(Vm.TestNet)
    private val engine = MappingEngine()
    private val violasRpcService = DataRepository.getViolasChainRpcService()
    private val libraRpcService = DataRepository.getLibraRpcService()

    init {
        engine.clearProcessors()
        engine.addProcessor(
            BitcoinToMappingCoinProcessor(
                contract.getContractAddress(),
                violasRpcService
            )
        )
        engine.addProcessor(LibraToMappingCoinProcessor(violasRpcService))
        engine.addProcessor(ViolasToOriginalCoinProcessor(libraRpcService))
    }

    suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAccountDO: AccountDO,
        payerAccountDO: AccountDO,
        password: ByteArray,
        amount: Long,
        coinPair: MappingCoinPairDTO
    ): String {
        return engine.mapping(
            checkPayeeAccount,
            payeeAccountDO,
            payerAccountDO,
            password,
            amount,
            coinPair
        )
    }
}