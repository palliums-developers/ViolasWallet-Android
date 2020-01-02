package androidx.lifecycle

/**
 * Created by elephant on 2020-01-02 17:01.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 加强版的[MutableLiveData]
 */
open class EnhancedMutableLiveData<T> : MutableLiveData<DataWrapper<T>> {

    constructor(value: T) : super(DataWrapper(value))

    constructor() : super()

    open fun postValueSupport(value: T) {
        postValue(DataWrapper(value))
    }

    open fun setValueSupport(value: T) {
        setValue(DataWrapper(value))
    }
}