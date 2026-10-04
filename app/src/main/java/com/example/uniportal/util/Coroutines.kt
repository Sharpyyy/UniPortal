package com.example.uniportal.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted

inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(5_000)
