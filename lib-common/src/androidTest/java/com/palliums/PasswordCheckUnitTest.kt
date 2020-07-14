package com.palliums

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.palliums.content.ContextProvider
import com.palliums.utils.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordCheckUnitTest {

    @Before
    fun sss(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        ContextProvider.init(appContext)
    }

    @Test(expected = PasswordValidationFailsException::class)
    fun testNumber() {
        val check = PasswordCheckUtil.check("12345678")
    }

    @Test(expected = PasswordEmptyException::class)
    fun testEmpty() {
        val check = PasswordCheckUtil.check("")
    }

    @Test(expected = PasswordValidationFailsException::class)
    fun testLowercase() {
        val check = PasswordCheckUtil.check("asdcvbnasdasd")
    }

    @Test(expected = PasswordValidationFailsException::class)
    fun testLowercaseUppercase() {
        val check = PasswordCheckUtil.check("ABDCNVMasdasd")
    }

    @Test(expected = PasswordValidationFailsException::class)
    fun testUppercase() {
        val check = PasswordCheckUtil.check("LAJSDLASLDJALSJD")
    }

    @Test(expected = PasswordValidationFailsException::class)
    fun testUppercaseANDNumber() {
        val check = PasswordCheckUtil.check("LAJSDLASL123123JD")
    }

    @Test(expected = PasswordValidationFailsException::class)
    fun testLowercaseANDNumber() {
        val check = PasswordCheckUtil.check("asdasdasfg123123")
    }

    @Test(expected = PasswordLengthShortException::class)
    fun testLengthShort() {
        val check = PasswordCheckUtil.check("asd")
    }

    @Test(expected = PasswordLengthLongException::class)
    fun testLengthLong() {
        val check = PasswordCheckUtil.check("asdasdasdasdasdasdasdasdasdasd")
    }

    @Test
    fun testNormal() {
        val check = PasswordCheckUtil.check("asdASD123")
        Assert.assertEquals(check, true)
    }

    @Test
    fun testNormalLen8() {
        val check = PasswordCheckUtil.check("asdASD13")
        Assert.assertEquals(check, true)
    }

    @Test
    fun testNormalLen20() {
        val check = PasswordCheckUtil.check("asdASD123asdASD12322")
        Assert.assertEquals(check, true)
    }

    @Test
    fun testSpecialCharacters() {
        val check = PasswordCheckUtil.check("asdASD123!@")
        Assert.assertEquals(check, true)
    }

    @Test(expected = PasswordSpecialFailsException::class)
    fun testFalseSpecialCharacters() {
        val check = PasswordCheckUtil.check("asdASD123!@",false)
        Assert.assertEquals(check, true)
    }
}
