package com.palliums.exceptions

/**
 * Created by elephant on 2020/5/14 11:22.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface BaseException {

    /**
     * 获取错误信息
     *
     * @param loadAction true: 表示是加载数据动作；false: 表示是其它操作动作
     */
    fun getErrorMessage(loadAction: Boolean): String
}