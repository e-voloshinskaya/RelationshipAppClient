package com.example.myapplication.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemSettingBinding
import com.example.myapplication.databinding.ItemSettingHeaderBinding
import com.example.myapplication.presentation.models.SettingsItem
import com.example.myapplication.presentation.models.SettingsSection

class SettingsAdapter(
    private val onItemClick: (SettingsItem) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(SettingsDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is String -> VIEW_TYPE_HEADER
            is SettingsItem -> VIEW_TYPE_ITEM
            else -> throw IllegalArgumentException("Unknown view type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemSettingHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_ITEM -> {
                val binding = ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ItemViewHolder(binding, onItemClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is String -> (holder as HeaderViewHolder).bind(item)
            is SettingsItem -> (holder as ItemViewHolder).bind(item)
        }
    }

    fun submitSections(sections: List<SettingsSection>) {
        val flattenedList = mutableListOf<Any>()
        sections.forEach { section ->
            flattenedList.add(section.title)
            flattenedList.addAll(section.items)
        }
        super.submitList(flattenedList)
    }

    class HeaderViewHolder(
        private val binding: ItemSettingHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.settingsHeader.text = title
        }
    }

    class ItemViewHolder(
        private val binding: ItemSettingBinding,
        private val onItemClick: (SettingsItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsItem) {
            binding.textSettingTitle.text = item.title

            // Настройка иконки слева
            if (item.iconResId != null) {
                binding.iconSetting.setImageResource(item.iconResId)
                binding.iconSetting.visibility = View.VISIBLE
            } else {
                binding.iconSetting.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private class SettingsDiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is String && newItem is String -> oldItem == newItem
                oldItem is SettingsItem && newItem is SettingsItem -> oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is String && newItem is String -> oldItem == newItem
                oldItem is SettingsItem && newItem is SettingsItem -> oldItem == newItem
                else -> false
            }
        }
    }
}