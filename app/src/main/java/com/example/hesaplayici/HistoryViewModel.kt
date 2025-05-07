package com.example.hesaplayici

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("history", Context.MODE_PRIVATE)
    val historyList = MutableLiveData<MutableList<String>>()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val historyString = sharedPreferences.getString("history", "") ?: ""
        val list = if (historyString.isNotEmpty()) historyString.split(",").toMutableList() else mutableListOf()
        historyList.value = list
    }

    fun saveHistory(list: MutableList<String>) {
        val historyString = list.joinToString(",")
        sharedPreferences.edit().putString("history", historyString).apply()
        historyList.value = list
    }

    fun clearHistory() {
        sharedPreferences.edit().remove("history").apply()
        historyList.value = mutableListOf()
    }
}