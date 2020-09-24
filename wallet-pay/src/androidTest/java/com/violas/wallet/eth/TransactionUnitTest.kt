package com.violas.wallet.eth

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.Transaction
import com.quincysx.crypto.bip32.ExtendedKey
import com.quincysx.crypto.bip44.BIP44
import com.quincysx.crypto.bip44.CoinPairDerive
import com.quincysx.crypto.ethereum.EthECKeyPair
import com.quincysx.crypto.ethereum.EthTransaction
import com.quincysx.crypto.utils.HexUtils
import com.violas.wallet.biz.MnemonicException
import com.violas.wallet.biz.eth.ERC20Contract
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
            val nonce = etherscanService.getNonce(from)
            Log.d("send transaction nonce", nonce.toString())
            val gasPrice = etherscanService.getGasPrice()
            Log.d("send transaction gasPrice", gasPrice.toString())

            // 估算 gas 费
            val gasLimit = etherscanService.estimateGas(
                to,
                from = from,
                value = amount
            )
            Log.d("send transaction gasLimit", gasLimit.toString())

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
            val erc20Contract = ERC20Contract("972c041a3044da600a5b087b1488cbe7665b38b1")
            val key = getKey()

            val from = "1A5DDDf83BCeE2360C966Ac23Ef220C7534772d5"
            val to = "0x4181376Fb691Ed8550eA444bce5E04a993B39B27"

            val amount =
                BigDecimal("0").multiply(BigDecimal("1000000000000000000")).toBigInteger()
            val nonce = etherscanService.getNonce(from)
            Log.d("send transaction nonce", nonce.toString())
            val gasPrice = etherscanService.getGasPrice()
            Log.d("send transaction gasPrice", gasPrice.toString())
            val chainId = 3

            // 生成交易 Code
            val contractCallResult = erc20Contract.transfer(to, BigInteger("1000000000000000000"))

            // 估算 gas 费
            val gasLimit = etherscanService.estimateGas(
                erc20Contract.contractAddress,
                from = from,
                data = contractCallResult.getData()
            )
            Log.d("send transaction gasLimit", gasLimit.toString())

            val transaction: Transaction = EthTransaction
                .create(
                    erc20Contract.contractAddress,  //合约地址
                    amount,  //转账金额
                    nonce,
                    gasPrice,
                    gasLimit,
                    contractCallResult.getData(),
                    chainId
                )

            val sign = transaction.sign(key)
            Log.d("send transaction sign tx", sign.toHex())
            val sendRawTransaction = etherscanService.sendRawTransaction(sign.toHex())
            Log.d("send transaction", sendRawTransaction ?: "")
        }
    }

    @Test
    fun test_erc20_contract() {
        runBlocking {
            val erC20Contract = ERC20Contract("972c041a3044da600a5b087b1488cbe7665b38b1")
            val name = erC20Contract.name()
            val symbol = erC20Contract.symbol()
            val decimals = erC20Contract.decimals()
            val totalSupply = erC20Contract.totalSupply()
            val balanceOf = erC20Contract.balanceOf("0x1A5DDDf83BCeE2360C966Ac23Ef220C7534772d5")


            Log.e("contract name", "$name")
            Log.e("contract symbol", "$symbol")
            Log.e("contract decimals", "${decimals}")
            Log.e("contract totalSupply", "${totalSupply}")
            Log.e("contract balanceOf", "${balanceOf}")
        }
    }
}