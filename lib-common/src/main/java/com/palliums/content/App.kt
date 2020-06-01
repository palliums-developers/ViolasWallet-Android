package com.palliums.content

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import java.util.*

open class App : Application(), ViewModelStoreOwner {
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

        fun existsActivity(clazz: Class<out Activity>): Boolean {
            activityStore.forEach {
                if (it.javaClass == clazz) {
                    return true
                }
            }
            return false
        }
    }

    private val mViewModelStore by lazy {
        ViewModelStore()
    }

    override fun onCreate() {
        super.onCreate()
        initActivityStack()
        ContextProvider.init(this)
    }

    fun getTopActivity(): Activity? {
        if (activityStore.empty()) {
            return null
        }
        return activityStore.lastElement()
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

    override fun getViewModelStore() = mViewModelStore
}