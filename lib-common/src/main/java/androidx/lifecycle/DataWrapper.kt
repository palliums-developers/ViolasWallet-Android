package androidx.lifecycle

/**
 * Created by elephant on 2020-01-02 16:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class DataWrapper<out T>(private val data: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    @Synchronized
    fun getDataIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekData(): T = data
}