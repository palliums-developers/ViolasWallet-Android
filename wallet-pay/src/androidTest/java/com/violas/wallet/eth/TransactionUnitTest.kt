package com.violas.wallet.eth

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.Transaction
import com.quincysx.crypto.bip32.ExtendedKey
import com.quincysx.crypto.bip44.BIP44
import com.quincysx.crypto.bip44.CoinPairDerive
import com.quincysx.crypto.ethereum.CallTransaction
import com.quincysx.crypto.ethereum.EthECKeyPair
import com.quincysx.crypto.ethereum.EthTransaction
import com.quincysx.crypto.utils.HexUtils
import com.violas.wallet.biz.MnemonicException
import com.violas.wallet.repository.DataRepository
import com.violas.walletconnect.extensions.toHex
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.palliums.libracore.mnemonic.Mnemonic
import java.math.BigDecimal
import java.math.BigInteger


@RunWith(AndroidJUnit4::class)
class TransactionUnitTest {
    @Test
    fun test_generate_transaction() {
        val from = "4181376Fb691Ed8550eA444bce5E04a993B39B27"
        val to = "AEEF46DB4855E25702F8237E8f403FddcaF931C0"
        val amount = BigDecimal("0.001").multiply(BigDecimal("1000000000000000000")).toBigInteger()
        val nonce = BigInteger("0")
        val gasPrice = BigInteger("1000000")
        val gasLimit = BigInteger("21000000")
        val chainId = 3
        val transaction: Transaction = EthTransaction
            .create(
                to,  //对方地址不带 0x
                amount,  //转账金额
                nonce,
                gasPrice,
                gasLimit,
                chainId
            )
        Assert.assertEquals(
            HexUtils.toHex(transaction.signBytes),
            "eb80830f42408401406f4094aeef46db4855e25702f8237e8f403fddcaf931c087038d7ea4c6800080038080"
        )
    }

    fun getKey(): EthECKeyPair {
        val wordList = arrayListOf<String>(
            "velvet",
            "version",
            "sea",
            "near",
            "truly",
            "open",
            "blanket",
            "exchange",
            "leaf",
            "cupboard",
            "shinepoem"
        )
        val seed = Mnemonic.English()
            .toByteArray(wordList) ?: throw MnemonicException()
        val extendedKey = ExtendedKey.create(seed)
        val bip44Path =
            BIP44.m().purpose44().coinType(CoinTypes.Ethereum).account(0).external().address(0)
        val derive = CoinPairDerive(extendedKey).derive(bip44Path)
        return derive as EthECKeyPair
    }

    @Test
    fun test_generate_key() {
        val key = getKey()

        Assert.assertEquals(key.address, "1A5DDDf83BCeE2360C966Ac23Ef220C7534772d5")
        Assert.assertEquals(
            key.publicKey,
            "7265f16965229b29108a7bcfbee953156796d46e018c7304848da363ac2eac25225cfdc6cfa39f7745fff270492c23ae7c75b865818946c80cb73ffaa9a8fd0c"
        )
        Assert.assertEquals(
            key.privateKey,
            "d5f5c1cc003bcaf2aa7c0750021339185c85d1ffa33b2eded3968138cd929fcb"
        )
    }

    @Test
    fun test_sign_transaction() {
        val key = getKey()

        val from = "4181376Fb691Ed8550eA444bce5E04a993B39B27"
        val to = "AEEF46DB4855E25702F8237E8f403FddcaF931C0"
        val amount = BigDecimal("0.001").multiply(BigDecimal("1000000000000000000")).toBigInteger()
        val nonce = BigInteger("0")
        val gasPrice = BigInteger("1000000")
        val gasLimit = BigInteger("21000000")
        val chainId = 3
        val transaction: Transaction = EthTransaction
            .create(
                to,  //对方地址不带 0x
                amount,  //转账金额
                nonce,
                gasPrice,
                gasLimit,
                chainId
            )

        val sign = transaction.sign(key)
        Assert.assertEquals(
            HexUtils.toHex(sign),
            "f86b80830f42408401406f4094aeef46db4855e25702f8237e8f403fddcaf931c087038d7ea4c68000802aa0b46ac080672456f76a8c3e7ee22ec8ea737394ca9a5a474659986a883902a6b9a02da0141e9f25f2e5b794c7dfb424c71c6559669bcc778f67676eadb33ae3b236"
        )
    }

    @Test
    fun test_send_transaction() {
        runBlocking {
            val etherscanService = DataRepository.getEtherscanService()
            val key = getKey()

            val from = "1A5DDDf83BCeE2360C966Ac23Ef220C7534772d5"
            val to = "4181376Fb691Ed8550eA444bce5E04a993B39B27"
            val amount =
                BigDecimal("0.001").multiply(BigDecimal("1000000000000000000")).toBigInteger()
            val nonce = BigInteger(etherscanService.getNonce(from).toString())
            Log.d("send transaction nonce", nonce.toString())
            val gasPrice = BigInteger(etherscanService.getGasPrice().toString())
            Log.d("send transaction gasPrice", gasPrice.toString())
            val gasLimit = BigInteger("21000")
            val chainId = 3
            val transaction: Transaction = EthTransaction
                .create(
                    to,  //对方地址不带 0x
                    amount,  //转账金额
                    nonce,
                    gasPrice,
                    gasLimit,
                    chainId
                )

            val sign = transaction.sign(key)
            Log.d("send transaction sign tx", sign.toHex())
            val sendRawTransaction = etherscanService.sendRawTransaction(sign.toHex())
            Log.d("send transaction", sendRawTransaction ?: "")
        }
    }

    @Test
    fun call_eth_erc20_transfer() {
        runBlocking {
            val etherscanService = DataRepository.getEtherscanService()
            val key = getKey()

            val from = "1A5DDDf83BCeE2360C966Ac23Ef220C7534772d5"
            val to = "4181376Fb691Ed8550eA444bce5E04a993B39B27"
            val amount =
                BigDecimal("0.001").multiply(BigDecimal("1000000000000000000")).toBigInteger()
            val nonce = BigInteger(etherscanService.getNonce(from).toString())
            Log.d("send transaction nonce", nonce.toString())
            val gasPrice = BigInteger(etherscanService.getGasPrice().toString())
            Log.d("send transaction gasPrice", gasPrice.toString())
            val gasLimit = BigInteger("42000")
            val chainId = 3

            //第一个参数是方法名，第二个参数是合约方法的参数类型(可以有多个)
            val function = CallTransaction.Function.fromSignature("transfer", "address", "uint256")

            val transaction: Transaction = CallTransaction
                .createCallTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    "972c041a3044da600a5b087b1488cbe7665b38b1",  //合约地址
                    BigInteger.valueOf(0), // 转账金额
                    function,
                    "1A5DDDf83BCeE2360C966Ac23Ef220C7534772d5",
                    1 * 1000000000000000000L//合约参数，可传很多参数(具体参数要和上述类型数量一致)
                )

            val sign = transaction.sign(key)
            Log.d("send transaction sign tx", sign.toHex())
            val sendRawTransaction = etherscanService.sendRawTransaction(sign.toHex())
            Log.d("send transaction", sendRawTransaction ?: "")
        }
    }

    @Test
    fun call_eth_erc20_balance() {
        runBlocking {
            val etherscanService = DataRepository.getEtherscanService()

            //第一个参数是方法名，第二个参数是合约方法的参数类型(可以有多个)
            val function = CallTransaction.Function.fromSignature("balanceOf", "address")
            val encode = function.encode("0x1A5DDDf83BCeE2360C966Ac23Ef220C7534772d5")

            val result = etherscanService.contractCall(
                "972c041a3044da600a5b087b1488cbe7665b38b1",
                data = encode.toHex()
            )

            val balance = BigInteger(result?.replace("0x", "") ?: "0", 16)
            Log.d("get balance", "${balance}")
        }
    }
}