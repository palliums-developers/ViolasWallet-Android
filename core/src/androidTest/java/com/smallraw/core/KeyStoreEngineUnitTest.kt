package com.smallraw.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smallraw.core.keystoreCompat.KeystoreCompat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyStoreEngineUnitTest {
    @Test
    fun test_create_key() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val content = "123".toByteArray()

        val keystoreCompat = KeystoreCompat(appContext)
        val encrypt = keystoreCompat.encrypt(content)
        val decrypt = keystoreCompat.decrypt(encrypt!!)

        Assert.assertArrayEquals(decrypt, content)
    }
}