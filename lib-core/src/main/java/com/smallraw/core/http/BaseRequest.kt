package com.smallraw.core.http

data class BaseRequest<T>(
    val code: Int,
    val `data`: T?,
    val message: String
)