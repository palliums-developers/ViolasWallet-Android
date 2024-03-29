package com.palliums.content

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.*

open class App : Application() {
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
        ContextProvider.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        while (!activityStore.isEmpty()) {
            activityStore.pop()
        }
    }

    private fun initActivityStack() {
        this.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                activityStore.add(activity)
            }

            override fun onActivityStarted(activity: Activity?) {
            }

            override fun onActivityResumed(activity: Activity?) {

            }

            override fun onActivityPaused(activity: Activity?) {

            }

            override fun onActivityStopped(activity: Activity?) {
            }

            override fun onActivityDestroyed(activity: Activity?) {
                activityStore.remove(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
            }
        })
    }
}