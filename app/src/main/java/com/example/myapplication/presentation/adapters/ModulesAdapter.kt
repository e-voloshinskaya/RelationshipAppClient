package com.example.myapplication.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.entity.ModuleItem
import com.example.myapplication.data.entity.ModuleStatus
import com.example.myapplication.databinding.ItemModuleBinding

// Используем ListAdapter для автоматизации и производительности
class ModulesAdapter(
    private val onModuleClicked: (ModuleItem) -> Unit
) : ListAdapter<ModuleItem, ModulesAdapter.ModuleViewHolder>(ModuleDiffCallback()) {

    // ViewHolder будет хранить ссылки на view из layout
    inner class ModuleViewHolder(private val binding: ItemModuleBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(moduleItem: ModuleItem) {
            binding.textModuleTitle.text = moduleItem.title

            // Получаем правильное окончание слова
            val sectionsText = getPluralizedForm(
                moduleItem.sectionsCount,
                "раздел",
                "раздела",
                "разделов"
            )

            // Устанавливаем текст вида: "3 раздела"
            binding.textModuleSectionsCount.text = itemView.context.getString(
                R.string.module_sections_count,
                moduleItem.sectionsCount,
                sectionsText
            )

            // Логика отображения статуса
            when (moduleItem.status) {
                ModuleStatus.COMPLETED -> {
                    binding.imageModuleStatus.visibility = View.VISIBLE
                    binding.imageModuleInProgress.visibility = View.GONE
                    binding.colorStripe.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.green_mild)
                    )
                }
                ModuleStatus.IN_PROGRESS -> {
                    binding.imageModuleStatus.visibility = View.GONE
                    binding.imageModuleInProgress.visibility = View.VISIBLE
                    binding.colorStripe.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                    )
                }
                ModuleStatus.NO_STATUS -> {
                    binding.imageModuleStatus.visibility = View.GONE
                    binding.imageModuleInProgress.visibility = View.GONE
                    binding.colorStripe.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                    )
                }
            }

            // Устанавливаем обработчик клика
            itemView.setOnClickListener {
                onModuleClicked(moduleItem)
            }
        }
    }

    // Стандартные методы для ListAdapter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // --- Вспомогательная функция для склонения слов ---
    private fun getPluralizedForm(count: Int, one: String, few: String, many: String): String {
        if (count % 100 in 11..19) return many
        return when (count % 10) {
            1 -> one
            2, 3, 4 -> few
            else -> many
        }
    }

    // DiffUtil помогает RecyclerView понять, какие элементы изменились, добавились или удалились
    class ModuleDiffCallback : DiffUtil.ItemCallback<ModuleItem>() {
        override fun areItemsTheSame(oldItem: ModuleItem, newItem: ModuleItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ModuleItem, newItem: ModuleItem): Boolean {
            return oldItem == newItem
        }
    }
}