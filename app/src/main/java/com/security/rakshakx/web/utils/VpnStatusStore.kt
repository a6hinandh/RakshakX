package com.security.rakshakx.web.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VpnStatusStore {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }
}
