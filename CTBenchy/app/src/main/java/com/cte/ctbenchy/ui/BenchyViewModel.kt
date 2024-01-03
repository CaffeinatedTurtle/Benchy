package com.cte.ctbenchy.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BenchyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BenchyState())
    val uiState: StateFlow<BenchyState> = _uiState.asStateFlow()

    fun setLedMask(mask: Byte) {
        _uiState.update { currentState ->
            currentState.copy(ledMask = mask)
        }
    }

    fun setConnectionState(state: Int) {
        _uiState.update { currentState ->
            currentState.copy(connectState = state)
        }
    }

    fun setMode(mode: Byte) {
        _uiState.update { currentState ->
            currentState.copy(mode = mode)
        }
    }

    fun setThrottle(throttle: Short) {
        _uiState.update { currentState ->
            currentState.copy(throttle = throttle.toInt())
        }
    }

    fun setRudder(rudder: Short) {
        _uiState.update { currentState ->
            currentState.copy(rudder = rudder.toInt())
        }
    }
}