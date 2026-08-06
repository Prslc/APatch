package me.bmax.apatch.ui.page.home

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** Lightweight event bus: APM/KPM pages signal count changes, HomeViewModel listens. */
object ModuleCountsRefresher {
    private val _events = Channel<Unit>(Channel.CONFLATED)
    val events = _events.receiveAsFlow()

    fun requestRefresh() {
        _events.trySend(Unit)
    }
}
