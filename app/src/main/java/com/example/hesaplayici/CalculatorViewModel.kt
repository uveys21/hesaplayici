package com.example.hesaplayici

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle

class CalculatorViewModel(
    private val savedStateHandle: SavedStateHandle,
    application: Context
) : AndroidViewModel(application as Application) {

    val currentNumberFormat: MutableLiveData<String> = MutableLiveData("standard")

    private val _currentInput = MutableLiveData(savedStateHandle.get<String>(CURRENT_INPUT) ?: "")
    val currentInput: LiveData<String> = _currentInput

    private val _history = MutableLiveData(savedStateHandle.get<String>(HISTORY) ?: "")
    val history: LiveData<String> = _history

    private val _displayFormat = MutableLiveData<String>()
    val displayFormat: LiveData<String> = _displayFormat

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("settings", Application.MODE_PRIVATE)

    init {
        loadDisplayFormat()
    }

    private fun loadDisplayFormat() {
        val format = sharedPreferences.getString("display_format", "standard") ?: "standard"
        _displayFormat.value = format
    }

    fun setDisplayFormat(format: String) {
        _displayFormat.value = format
        sharedPreferences.edit().putString("display_format", format).apply()
    }

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
