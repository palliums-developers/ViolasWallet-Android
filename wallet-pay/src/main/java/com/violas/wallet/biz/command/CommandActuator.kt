package com.violas.wallet.biz.command

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import java.util.*

object CommandActuator {
    private val mHandlerThread by lazy {
        HandlerThread("CommandActuator-Thread")
    }
    private val mHandler by lazy {
        mHandlerThread.start()
        Handler(mHandlerThread.looper)
    }
    private val mCommandMap = WeakHashMap<String, Runnable>()

    fun post(command: ICommand) {
        postDelay(command, 0)
    }

    /**
     * @param delayMillis The delay (in milliseconds) until the ICommand
     */
    fun postDelay(command: ICommand, delayMillis: Long) {
        val runnable = Runnable {
            command.exec()
            if (command is ISingleCommand) {
                val identity = command.getIdentity()
                mCommandMap.remove(identity)
            }
        }
        var isSingRunning = false
        if (command is ISingleCommand) {
            val identity = command.getIdentity()
            val singleCommand = mCommandMap[identity]

            isSingRunning = (singleCommand != null
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && mHandler.hasCallbacks(singleCommand))

            if (singleCommand != null && !isSingRunning) {
                mHandler.removeCallbacks(singleCommand)
            }

            if (isSingRunning) {
                // 如果上一次的任务正在执行则不记录任务信息
                return
            }
            mCommandMap[identity] = runnable
        }
        if (isSingRunning) {
            // 如果上一次的任务正在执行则不添加新的任务到队列
            return
        }
        mHandler.postDelayed(runnable, delayMillis)
    }
}