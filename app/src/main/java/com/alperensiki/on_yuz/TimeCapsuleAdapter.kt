package com.alperensiki.on_yuz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alperensiki.on_yuz.R
import com.alperensiki.on_yuz.TimeCapsuleRoom

class TimeCapsuleAdapter(
    private val rooms: List<TimeCapsuleRoom>,
    private val onItemClick: (TimeCapsuleRoom) -> Unit
) : RecyclerView.Adapter<TimeCapsuleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDays: TextView = view.findViewById(R.id.tvDays)
        val tvHours: TextView = view.findViewById(R.id.tvHours)
        val tvTitle: TextView = view.findViewById(R.id.tvRoomTitle)
        val tvNotify: TextView = view.findViewById(R.id.tvNotifyInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_capsule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = rooms[position]
        holder.tvTitle.text = room.roomName
        holder.tvDays.text = room.capsuleDays.toString()
        holder.tvHours.text = room.capsuleHours.toString()

        val notifyText = when {
            room.notificationDays > 0 && room.notificationHours > 0 -> "${room.notificationDays}d ${room.notificationHours}h"
            room.notificationDays > 0 -> "${room.notificationDays} Days"
            else -> "${room.notificationHours} Hours"
        }
        holder.tvNotify.text = notifyText

        // Tıklama olayını burada onItemClick içine gönderiyoruz
        holder.itemView.setOnClickListener {
            onItemClick(room)
        }
    }

    override fun getItemCount() = rooms.size
}