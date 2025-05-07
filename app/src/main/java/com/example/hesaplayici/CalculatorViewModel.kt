package com.example.hesaplayici

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class CalculatorViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _currentInput = MutableLiveData(savedStateHandle.get<String>(CURRENT_INPUT) ?: "")
    val currentInput: LiveData<String> = _currentInput

    private val _history = MutableLiveData(savedStateHandle.get<String>(HISTORY) ?: "")
    val history: LiveData<String> = _history

    fun setCurrentInput(input: String) {
        _currentInput.value = input
        savedStateHandle.set(CURRENT_INPUT, input)
    }

    fun setHistory(history: String) {
        _history.value = history
        savedStateHandle.set(HISTORY, history)
    }

    companion object {
        private const val CURRENT_INPUT = "currentInput"
        private const val HISTORY = "history"
    }
}