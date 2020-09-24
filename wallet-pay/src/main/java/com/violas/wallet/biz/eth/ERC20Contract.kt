package com.violas.wallet.biz.eth

import com.quincysx.crypto.ethereum.CallTransaction
import com.violas.wallet.repository.DataRepository
import com.violas.walletconnect.extensions.toHex
import org.palliums.libracore.serialization.hexToBytes
import java.math.BigInteger

class ERC20Contract(val contractAddress: String) {

    private val mEtherscanService by lazy {
        DataRepository.getEtherscanService()
    }

    private fun String.addHexPrefix(): String {
        return if (this[0] == '0' && this[1] == 'x') {
            this
        } else {
            "0x$this"
        }
    }

    private fun String.removeHexPrefix(): String {
        return if (this[0] == '0' && this[1] == 'x') {
            return this.removePrefix("0x")
        } else {
            this
        }
    }

    suspend fun name(): String {
        val function = CallTransaction.Function.fromSignature("name", arrayOf(), arrayOf("string"))
        val result = mEtherscanService.contractCall(
            contractAddress, data = function.encode()
        )
        val decodeResult = function.decodeResult(result?.replace("0x", "")?.hexToBytes())
        return decodeResult[0] as String
    }

    suspend fun symbol(): String {
        val function =
            CallTransaction.Function.fromSignature("symbol", arrayOf(), arrayOf("string"))
        val result = mEtherscanService.contractCall(
            contractAddress, data = function.encode()
        )
        val decodeResult = function.decodeResult(result?.replace("0x", "")?.hexToBytes())
        return decodeResult[0] as String
    }

    suspend fun decimals(): Int {
        val function =
            CallTransaction.Function.fromSignature("decimals", arrayOf(), arrayOf("uint8"))
        val result = mEtherscanService.contractCall(
            contractAddress, data = function.encode()
        )
        val decodeResult = function.decodeResult(result?.replace("0x", "")?.hexToBytes())
        return (decodeResult[0] as BigInteger).toInt()
    }

    suspend fun totalSupply(): BigInteger {
        val function =
            CallTransaction.Function.fromSignature("totalSupply", arrayOf(), arrayOf("uint256"))
        val result = mEtherscanService.contractCall(
            contractAddress, data = function.encode()
        )
        val decodeResult = function.decodeResult(result?.replace("0x", "")?.hexToBytes())
        return decodeResult[0] as BigInteger
    }

    suspend fun balanceOf(address: String): BigInteger {
        val function =
            CallTransaction.Function.fromSignature(
                "balanceOf",
                arrayOf("address"),
                arrayOf("uint256")
            )
        val result = mEtherscanService.contractCall(
            contractAddress, data = function.encode(address)
        )
        val decodeResult = function.decodeResult(result?.replace("0x", "")?.hexToBytes())
        return decodeResult[0] as BigInteger
    }

    fun transfer(toAddress: String, amount: BigInteger): ContractCallResult {
        val function =
            CallTransaction.Function.fromSignature(
                "transfer",
                arrayOf("address", "uint256"),
                arrayOf("bool")
            )
        return ContractCallResult(function, function.encode(toAddress, amount))
    }

    fun transferFrom(
        fromAddress: String,
        toAddress: String,
        amount: BigInteger
    ): ContractCallResult {
        val function =
            CallTransaction.Function.fromSignature(
                "transferFrom",
                arrayOf("address", "address", "uint256"),
                arrayOf("bool")
            )
        return ContractCallResult(function, function.encode(fromAddress, toAddress, amount))
    }

    fun approve(toAddress: String, amount: BigInteger): ContractCallResult {
        val function =
            CallTransaction.Function.fromSignature(
                "approve",
                arrayOf("address", "uint256"),
                arrayOf("bool")
            )
        return ContractCallResult(function, function.encode(toAddress, amount))
    }

    fun allowance(ownerAddress: String, spenderAddress: String): ContractCallResult {
        val function =
            CallTransaction.Function.fromSignature(
                "allowance",
                arrayOf("address", "address"),
                arrayOf("uint256")
            )
        return ContractCallResult(function, function.encode(ownerAddress, spenderAddress))
    }
}

data class ContractCallResult(
    private val function: CallTransaction.Function,
    private val data: ByteArray
) {
    fun decode(byteArray: ByteArray): Any {
        return function.decodeResult(byteArray)
    }

    fun getData() = data
}