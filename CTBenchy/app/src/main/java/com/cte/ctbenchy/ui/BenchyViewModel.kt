package com.cte.ctbenchy.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BenchyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BenchyState())
    val uiState: StateFlow<BenchyState> = _uiState.asStateFlow()

 fun setLedMask(mask:Byte){
     _uiState.update { currentState ->
         currentState.copy(ledMask = mask)
     }
 }
}