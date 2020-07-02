package com.violas.wallet

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.ISingleCommand
import org.junit.Test
import org.junit.runner.RunWith

class CommandTest(private val taskName: String) : ISingleCommand {
    override fun exec() {
        try {
            Log.e("CommandUnitTest", "$taskName 任务开始")
            Thread.sleep(5000)
            Log.e("CommandUnitTest", "$taskName 任务开始")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun getIdentity(): String {
        return "sign"
    }
}

@RunWith(AndroidJUnit4::class)
class CommandUnitTest {

    @Test
    fun testCommandActuator() {
        // Context of the app under test.
        CommandActuator.postDelay(CommandTest("task1"), 2000)
        CommandActuator.postDelay(CommandTest("task2"), 3000)
        CommandActuator.postDelay(CommandTest("task3"), 4000)
    }
}