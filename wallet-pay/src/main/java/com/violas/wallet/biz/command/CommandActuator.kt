package com.violas.wallet.biz.command

import android.os.Handler
import android.os.HandlerThread

object CommandActuator {
    private val mHandlerThread by lazy {
        HandlerThread("CommandActuator-Thread")
    }
    private val mHandler by lazy {
        mHandlerThread.start()
        Handler(mHandlerThread.looper)
    }
    private val mCommandMap = HashMap<String, Runnable>()

    fun post(command: ICommand) {
        postDelay(command, 0)
    }

    /**
     * @param delayMillis The delay (in milliseconds) until the ICommand
     */
    fun postDelay(command: ICommand, delayMillis: Long) {
        val runnable = Runnable {
            command.exec()
        }
        if (command is ISingleCommand) {
            val identity = command.getIdentity()
            val singleCommand = mCommandMap[identity]
            if (singleCommand != null) {
                mHandler.removeCallbacks(singleCommand)
            }
            mCommandMap[identity] = runnable
        }
        mHandler.postDelayed(runnable, delayMillis)
    }
}