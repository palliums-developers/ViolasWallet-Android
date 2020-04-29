package com.violas.wallet.sso.transaction

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.violas.wallet.biz.TokenManager
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.wallet.Account
import org.palliums.violascore.crypto.KeyPair

@RunWith(AndroidJUnit4::class)
class TokenChainUnitTest {
    private val mTokenManager by lazy {
        TokenManager()
    }

    private fun getAccount(): Account {
        val privateKey = "20adbc30341c9403103395cf3075089f25bb8edd8b00aabc3074fb176b5b2d54"

        return Account(KeyPair.fromSecretKey(privateKey.hexToBytes()))
    }

    @Test
    fun test_token_register() {
        val account = getAccount()
        val tokenAddress = account.getAddress().toHex()

        val result = runBlocking {
            try {
                mTokenManager.isPublishedContract(tokenAddress)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        assert(result)
    }

    //    @Test
    fun test_token_publish() {
        val account = getAccount()

        val result = runBlocking {
            try {
                mTokenManager.publishContract(account)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        assert(result)
    }

    //    @Test
    fun test_token_mint() {
        val account = getAccount()
        val tokenAddress = account.getAddress().toHex()
        val receiveAddress = account.getAddress().toHex()
        val receiveAmount = 10000 * 1000000L

        val result = runBlocking {
            try {
                mTokenManager.mintToken(account, 1, receiveAddress, receiveAmount)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        assert(result)
    }
}