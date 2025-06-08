// presentation/adapters/NotificationsAdapter.kt
package com.example.myapplication.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemNotificationBinding
import com.example.myapplication.presentation.models.AppNotification

class NotificationsAdapter : ListAdapter<AppNotification, NotificationsAdapter.NotificationViewHolder>(
    NotificationDiffCallback()
) {

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // В NotificationsAdapter
        fun bind(notification: AppNotification) {
            binding.textMessage.text = notification.message
            binding.textDate.text = notification.date
            binding.unreadIndicator.isVisible = !notification.isRead

            // Устанавливаем иконку в зависимости от типа
            val iconRes = when (notification.type) {
                "test_completed" -> R.drawable.ic_notes
                "module_completed" -> R.drawable.ic_book_open
                "lovemap_updated" -> R.drawable.ic_heart
                else -> R.drawable.ic_bell
            }
            // binding.iconType.setImageResource(iconRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(
            oldItem: AppNotification,
            newItem: AppNotification
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AppNotification,
            newItem: AppNotification
        ): Boolean {
            return oldItem == newItem
        }
    }
}