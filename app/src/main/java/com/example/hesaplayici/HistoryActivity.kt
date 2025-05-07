package com.example.hesaplayici

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var viewModel: HistoryViewModel
    private lateinit var clearAllButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        viewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)

        viewModel.historyList.observe(this) { history ->
            historyAdapter = HistoryAdapter(history.toMutableList()) { position ->
                val mutableHistory = viewModel.historyList.value?.toMutableList()
                mutableHistory?.removeAt(position)
                mutableHistory?.let { viewModel.saveHistory(it) }
            }
            recyclerView.adapter = historyAdapter
        }

        clearAllButton = findViewById(R.id.clearAllButton)
        clearAllButton.setOnClickListener {
            viewModel.historyList.value = mutableListOf()
            viewModel.saveHistory(mutableListOf())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}