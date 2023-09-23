package com.cte.ctbenchy.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BenchyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BenchyState())
    val uiState: StateFlow<BenchyState> = _uiState.asStateFlow()
}