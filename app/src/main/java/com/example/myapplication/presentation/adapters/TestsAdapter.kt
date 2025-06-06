import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import com.example.myapplication.databinding.ItemTestBlockBinding

/**
 * Адаптер для ViewPager2, который отображает список вопросов.
 * Каждый элемент списка - это отдельная страница с одним вопросом.
 */
class TestsAdapter(
    private val questions: List<Question>
) : RecyclerView.Adapter<TestsAdapter.QuestionViewHolder>() {

    /**
     * Создает новый ViewHolder (контейнер для одной страницы вопроса).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // Используем биндинг для макета одной страницы вопроса (item_question_page.xml)
        val binding = ItemTestBlockBinding.inflate(inflater, parent, false)
        return QuestionViewHolder(binding)
    }

    /**
     * Заполняет ViewHolder данными для конкретной позиции (вопроса).
     */
    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    /**
     * Возвращает общее количество вопросов.
     */
    override fun getItemCount(): Int = questions.size

    /**
     * ViewHolder - класс, который "держит" в себе все View одного элемента списка (одной страницы).
     * Вся логика по отображению конкретного типа вопроса находится здесь.
     */
    class QuestionViewHolder(private val binding: ItemTestBlockBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(question: Question) {
            // Устанавливаем текст вопроса
            binding.tvQuestionText.text = question.text

            // Сначала скрываем все контейнеры для вариантов ответов
            binding.rgSingleChoice.visibility = View.GONE
            binding.llMultipleChoice.visibility = View.GONE
            binding.tilFreeText.visibility = View.GONE

            // Очищаем от старых View, которые могли остаться от переиспользования ViewHolder
            binding.rgSingleChoice.removeAllViews()
            binding.llMultipleChoice.removeAllViews()
            binding.etFreeText.text = null

            // А теперь показываем и настраиваем нужный контейнер в зависимости от типа вопроса
            when (question.questionType) {
                // Типы с выбором одного ответа
                "single_choice", "survey_choice" -> {
                    binding.rgSingleChoice.visibility = View.VISIBLE
                    question.options.forEach { option ->
                        val radioButton = MaterialRadioButton(itemView.context).apply {
                            text = option.text
                            // Можно присвоить ID для дальнейшей идентификации
                            id = View.generateViewId()
                            // Задаем параметры, чтобы кнопка занимала всю ширину
                            layoutParams = RadioGroup.LayoutParams(
                                RadioGroup.LayoutParams.MATCH_PARENT,
                                RadioGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = 16 // Небольшой отступ снизу
                            }
                        }
                        binding.rgSingleChoice.addView(radioButton)
                    }
                }

                // Тип с выбором нескольких ответов
                "multiple_choice" -> {
                    binding.llMultipleChoice.visibility = View.VISIBLE
                    question.options.forEach { option ->
                        val checkBox = MaterialCheckBox(itemView.context).apply {
                            text = option.text
                            id = View.generateViewId()
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = 16
                            }
                        }
                        binding.llMultipleChoice.addView(checkBox)
                    }
                }

                // Тип со свободным вводом текста
                "free_text" -> {
                    binding.tilFreeText.visibility = View.VISIBLE
                }
            }
        }
    }
}