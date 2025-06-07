package com.example.myapplication.presentation.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import com.example.myapplication.databinding.ItemTestBlockBinding
import Question
import android.widget.RadioButton

/**
 * Адаптер, который отображает список вопросов во ViewPager2.
 * Каждый элемент списка - это отдельная страница с одним вопросом.
 */
class TestsAdapter(
    private val questions: List<Question>,
    // НОВЫЕ ПАРАМЕТРЫ: функции для передачи ответов "наружу" во ViewModel
    private val onOptionSelected: (questionId: String, optionId: String) -> Unit,
    private val onFreeTextChanged: (questionId: String, text: String) -> Unit
) : RecyclerView.Adapter<TestsAdapter.QuestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTestBlockBinding.inflate(inflater, parent, false)
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount(): Int = questions.size

    // ViewHolder теперь имеет доступ к onOptionSelected и onFreeTextChanged из конструктора адаптера
    inner class QuestionViewHolder(private val binding: ItemTestBlockBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(question: Question) {
            binding.tvQuestionText.text = question.text

            // Скрываем и очищаем все контейнеры перед настройкой
            binding.rgSingleChoice.isVisible = false
            binding.llMultipleChoice.isVisible = false
            binding.tilFreeText.isVisible = false
            binding.rgSingleChoice.removeAllViews()
            binding.llMultipleChoice.removeAllViews()
            binding.etFreeText.text = null

            when (question.questionType) {
                "single_choice", "survey_choice" -> {
                    binding.rgSingleChoice.isVisible = true
                    // Сбрасываем слушатель, чтобы избежать многократных вызовов при переиспользовании ViewHolder
                    binding.rgSingleChoice.setOnCheckedChangeListener(null)

                    question.options?.forEach { option ->
                        val radioButton = MaterialRadioButton(itemView.context).apply {
                            text = option.text
                            id = View.generateViewId()
                            // Сохраняем ID опции в тег, чтобы его можно было достать позже
                            tag = option.optionId

                            val paddingDp = 8
                            val paddingPx = (paddingDp * resources.displayMetrics.density).toInt()
                            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        }
                        binding.rgSingleChoice.addView(radioButton)
                    }

                    // Устанавливаем слушатель нажатий на группу кнопок
                    binding.rgSingleChoice.setOnCheckedChangeListener { group, checkedId ->
                        val checkedRadioButton = group.findViewById<RadioButton>(checkedId)
                        // Достаем ID опции из тега
                        val selectedOptionId = checkedRadioButton?.tag as? String
                        if (selectedOptionId != null) {
                            // Вызываем "внешнюю" функцию, передавая ID вопроса и ID выбранного ответа
                            onOptionSelected(question.questionId, selectedOptionId)
                        }
                    }
                }

                "multiple_choice" -> {
                    // Логика для множественного выбора (пока заглушка)
                    binding.llMultipleChoice.isVisible = true
                    question.options?.forEach { option ->
                        val checkBox = MaterialCheckBox(itemView.context).apply {
                            text = option.text
                            id = View.generateViewId()
                            tag = option.optionId
                            val paddingDp = 8
                            val paddingPx = (paddingDp * resources.displayMetrics.density).toInt()
                            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

                            // Здесь нужна будет более сложная логика для сбора нескольких ID
                        }
                        binding.llMultipleChoice.addView(checkBox)
                    }
                }

                "free_text" -> {
                    binding.tilFreeText.isVisible = true

                    // Устанавливаем слушатель на изменение текста
                    binding.etFreeText.doAfterTextChanged { text ->
                        // Вызываем "внешнюю" функцию, передавая ID вопроса и введенный текст
                        onFreeTextChanged(question.questionId, text.toString())
                    }

                    // Показываем клавиатуру
                    binding.etFreeText.requestFocus()
                    val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    itemView.postDelayed({
                        imm.showSoftInput(binding.etFreeText, InputMethodManager.SHOW_IMPLICIT)
                    }, 100)
                }
            }
        }
    }
}
