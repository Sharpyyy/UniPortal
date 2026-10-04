package com.example.uniportal.testutil

import com.example.uniportal.data.sync.SyncReport
import com.example.uniportal.data.sync.UniversitySyncer

class FakeUniversitySyncer(
    var handler: suspend () -> SyncReport = { SyncReport(refreshed = 0, failed = 0, pruned = 0) },
) : UniversitySyncer {
    var syncCalls = 0
        private set

    override suspend fun sync(): SyncReport {
        syncCalls++
        return handler()
    }
}
