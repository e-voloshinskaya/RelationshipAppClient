package com.example.myapplication.presentation.adapters

import HistoryItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val onHistoryItemClicked: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            binding.textHistoryItemTitle.text = item.title
            binding.textHistoryItemDatetime.text = item.completedAt
            binding.textHistoryItemSubtype.text = "Практика" // Здесь можно добавить логику для типа

            // --- Логика отображения галочек ---

            // Твоя галочка (всегда видна и закрашена)
            binding.iconCheckmarkUser1.isVisible = true
            binding.iconCheckmarkUser1.setColorFilter(
                ContextCompat.getColor(itemView.context, R.color.blue_mild) // Твой цвет
            )

            // Галочка партнера
            when (item.partnerStatus) {
                CompletionStatus.COMPLETED -> {
                    binding.iconCheckmarkUser2.isVisible = true
                    binding.iconCheckmarkUser2.setImageResource(R.drawable.ic_check)
                    binding.iconCheckmarkUser2.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.green_mild) // Цвет партнера
                    )
                }
                CompletionStatus.NOT_COMPLETED -> {
                    // Показываем иконку "в процессе" (например, часы)
                    binding.iconCheckmarkUser2.isVisible = true
                    binding.iconCheckmarkUser2.setImageResource(R.drawable.ic_time)
                    binding.iconCheckmarkUser2.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                    )
                }
                CompletionStatus.NOT_APPLICABLE -> {
                    // Просто скрываем иконку партнера
                    binding.iconCheckmarkUser2.isVisible = false
                }
            }

            // Устанавливаем обработчик клика на всю карточку
            itemView.setOnClickListener {
                onHistoryItemClicked(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding =
            ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem.attemptId == newItem.attemptId
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}