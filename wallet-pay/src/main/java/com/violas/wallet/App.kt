package com.violas.wallet

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import java.util.*

class App : Application() {
    companion object {
        val activityStore = Stack<Activity>()

        fun finishAllActivity() {
            while (!activityStore.isEmpty()) {
                try {
                    activityStore.pop().finish()
                } catch (e: Exception) {
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initActivityStack()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        MultiLanguageUtility.init(newBase)
        MultiLanguageUtility.attachBaseContext(newBase)
    }

    override fun onTerminate() {
        super.onTerminate()
        while (!activityStore.isEmpty()) {
            activityStore.pop()
        }
    }

    private fun initActivityStack() {
        this.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {

            }

            override fun onActivityResumed(activity: Activity?) {

            }

            override fun onActivityStarted(activity: Activity?) {
            }

            override fun onActivityDestroyed(activity: Activity?) {
                activityStore.remove(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
            }

            override fun onActivityStopped(activity: Activity?) {
            }

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                activityStore.add(activity)
            }

        })
    }
}