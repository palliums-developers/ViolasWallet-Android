package org.palliums.violascore.smartcontract

import org.palliums.violascore.smartcontract.multitoken.MultiContractRpcApi
import org.palliums.violascore.smartcontract.multitoken.MultiTokenContract

class ViolasMultiTokenContract(multiContractRpcApi: MultiContractRpcApi? = null) :
    MultiTokenContract("e1be1ab8360a35a0259f1c93e3eac736", multiContractRpcApi)