package com.github.allureidea.api

class AllureApiException(
    message: String,
    val statusCode: Int = 0,
    cause: Throwable? = null
) : RuntimeException(message, cause)
