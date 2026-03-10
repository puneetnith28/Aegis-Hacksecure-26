package com.obsidian.aegis.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.obsidian.aegis.databinding.ItemSuspiciousLogsBinding
import com.obsidian.aegis.models.SuspiciousActivity
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.obsidian.aegis.repository.SharedPrefManager

class SuspiciousLogsAdapter : RecyclerView.Adapter<SuspiciousLogsAdapter.SuspiciousLogViewHolder>() {

    inner class SuspiciousLogViewHolder(val binding: ItemSuspiciousLogsBinding) : RecyclerView.ViewHolder(binding.root)

    private val differCallback = object : DiffUtil.ItemCallback<SuspiciousActivity>() {
        override fun areItemsTheSame(oldItem: SuspiciousActivity, newItem: SuspiciousActivity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SuspiciousActivity, newItem: SuspiciousActivity): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuspiciousLogViewHolder {
        val binding = ItemSuspiciousLogsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuspiciousLogViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: SuspiciousLogViewHolder, position: Int) {
        val activity = differ.currentList[position]
        holder.binding.apply {
            tvAppName.text = activity.appName
            tvAppId.text = activity.appId
            tvDescription.text = activity.description
            tvRiskLevel.text = activity.riskLevel
            cardScreenOff.visibility = if (activity.isScreenOff) android.view.View.VISIBLE else android.view.View.GONE
            
            val color = when(activity.riskLevel.lowercase()) {
                "critical" -> "#F44336" // Red
                "high" -> "#FF9800" // Orange
                "tracking" -> "#2196F3" // Blue
                else -> "#757575" // Grey
            }
            tvRiskLevel.setBackgroundColor(Color.parseColor(color))

            val date = Date(activity.time)
            val timeFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvTimeStamp.text = timeFormat.format(date)
            tvDate.text = dateFormat.format(date)

            val context = root.context
            val sharedPrefManager = SharedPrefManager.getInstance(context)
            if (!sharedPrefManager.isAutoRevokeEnabled()) {
                btnRevoke.visibility = android.view.View.VISIBLE
                btnRevoke.setOnClickListener {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${activity.appId}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                    }
                }
            } else {
                btnRevoke.visibility = android.view.View.GONE
            }
        }
    }
}
