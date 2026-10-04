package com.example.uniportal.testutil

import com.example.uniportal.data.connectivity.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeConnectivityObserver(initiallyOnline: Boolean = true) : ConnectivityObserver {
    val online = MutableStateFlow(initiallyOnline)

    override val isOnline: Flow<Boolean>
        get() = online

    val activeObservers: Int
        get() = online.subscriptionCount.value

    fun goOffline() {
        online.value = false
    }

    fun goOnline() {
        online.value = true
    }
}
