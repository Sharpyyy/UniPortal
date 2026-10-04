package com.example.uniportal.work

import com.example.uniportal.data.sync.SyncReport

sealed interface SyncState {
    data object Idle : SyncState

    data object Waiting : SyncState

    data object Running : SyncState

    data class Succeeded(val report: SyncReport) : SyncState

    data object Failed : SyncState
}

sealed interface DownloadState {
    data object Waiting : DownloadState

    data class Running(val downloaded: Int) : DownloadState

    data class Succeeded(val downloaded: Int) : DownloadState

    data object Failed : DownloadState

    val isActive: Boolean
        get() = this is Waiting || this is Running
}
