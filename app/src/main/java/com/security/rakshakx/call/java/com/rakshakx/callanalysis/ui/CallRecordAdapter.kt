package com.rakshakx.callanalysis.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rakshakx.R

/**
 * Adapter for displaying list of recent calls (from call log) with analyze button.
 */
class CallRecordAdapter(
    private var calls: List<String>,
    private val onAnalyzeClick: (String) -> Unit
) : RecyclerView.Adapter<CallRecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val phoneText: TextView = view.findViewById(R.id.phoneText)
        val analyzeButton: Button = view.findViewById(R.id.analyzeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_record, parent, false) // Assume layout exists
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val phoneNumber = calls[position]
        holder.phoneText.text = phoneNumber
        holder.analyzeButton.setOnClickListener { onAnalyzeClick(phoneNumber) }
    }

    override fun getItemCount() = calls.size

    fun updateCalls(newCalls: List<String>) {
        calls = newCalls
        notifyDataSetChanged()
    }
}
