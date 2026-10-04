package com.example.uniportal.testutil

import com.example.uniportal.data.repository.HistoryRepository
import com.example.uniportal.domain.model.University
import kotlinx.coroutines.channels.Channel

class NotifyingHistoryRepository(
    private val delegate: HistoryRepository,
) : HistoryRepository by delegate {
    private val finishedViews = Channel<University>(Channel.UNLIMITED)

    override suspend fun recordView(university: University, limit: Int) {
        try {
            delegate.recordView(university, limit)
        } finally {
            finishedViews.trySend(university)
        }
    }

    suspend fun awaitRecordedView(): University = finishedViews.receive()
}
