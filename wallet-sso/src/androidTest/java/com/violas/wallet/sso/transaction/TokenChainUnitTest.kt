package com.violas.wallet.sso.transaction

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import com.violas.wallet.repository.DataRepository
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.wallet.Account

@RunWith(AndroidJUnit4::class)
class TokenChainUnitTest {
    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private fun getAccount(): Account {
        val privateKey = "20adbc30341c9403103395cf3075089f25bb8edd8b00aabc3074fb176b5b2d54"

        return Account(KeyPair.fromSecretKey(privateKey.hexToBytes()))
    }

    @Test
    fun test_token_register() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val account = getAccount()

        val tokenAddress = account.getAddress().toHex()

        mViolasService.tokenRegister(appContext, tokenAddress, account) {
            assert(it)
        }
    }

    //    @Test
    fun test_token_publish() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val account = getAccount()

        val tokenAddress = account.getAddress().toHex()

        mViolasService.publishToken(appContext, account, tokenAddress) {
            assert(it)
        }
    }

    //    @Test
    fun test_token_mint() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val account = getAccount()

        val tokenAddress = account.getAddress().toHex()
        val receiveAddress = account.getAddress().toHex()
        val receiveAmount = 10000 * 1000000L

        mViolasService.tokenMint(appContext, tokenAddress, account, receiveAddress, receiveAmount) {
            assert(it)
        }
    }
}