package com.example.hesaplayici

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CalculatorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val context: Context
) : ViewModel() {

    private val _currentInput = MutableLiveData(savedStateHandle.get<String>(CURRENT_INPUT) ?: "")
    val currentInput: LiveData<String> = _currentInput

    private val _history = MutableLiveData(savedStateHandle.get<String>(HISTORY) ?: "")
    val history: LiveData<String> = _history

    private val _displayFormat = MutableLiveData<String>()
    val displayFormat: LiveData<String> = _displayFormat

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    init {
        loadDisplayFormat()
    }

    private fun loadDisplayFormat() {
        val format = sharedPreferences.getString("display_format", "standard") ?: "standard"
        _displayFormat.value = format
    }

    fun setDisplayFormat(format: String) {
        _displayFormat.value = format
        with(sharedPreferences.edit()) {
            putString("display_format", format)
            apply()
        }
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