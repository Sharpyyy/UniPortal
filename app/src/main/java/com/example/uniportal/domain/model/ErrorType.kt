package com.example.uniportal.domain.model

import java.io.IOException
import retrofit2.HttpException

enum class ErrorType { NETWORK, SERVER, NOT_FOUND, UNKNOWN }

fun Throwable.toErrorType(): ErrorType = when (this) {
    is UniversityNotFoundException -> ErrorType.NOT_FOUND
    is IOException -> ErrorType.NETWORK
    is HttpException -> ErrorType.SERVER
    else -> ErrorType.UNKNOWN
}
