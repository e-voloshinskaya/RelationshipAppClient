import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

// Состояния UI для надежной отрисовки во фрагменте
sealed interface ModuleUiState {
    object Loading : ModuleUiState
    data class Success(val content: List<ModuleContentItem>) : ModuleUiState
    data class Error(val message: String) : ModuleUiState
}

class ModuleStepViewModel(
    private val supabase: SupabaseClient, // Фабрика подставит сюда SupabaseInit.client
    private val moduleId: String          // Фабрика подставит сюда moduleId из фрагмента
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModuleUiState>(ModuleUiState.Loading)
    val uiState: StateFlow<ModuleUiState> = _uiState

    // Хранит индекс текущего шага
    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _uiState.value = ModuleUiState.Loading
            Log.d("ModuleStepVM", "Загрузка контента для модуля: $moduleId")
            try {
                // ФИНАЛЬНЫЙ, ПРАВИЛЬНЫЙ ЗАПРОС
                val content = supabase
                    .from("Module_Content") // <-- Наша главная таблица!
                    .select(
                        columns = Columns.raw("""
                        id,
                        order,
                        content_type,

                        TheoryBlocks:theory_id(
                            theory_id,
                            theory_title,
                            content
                        ),

                        Tests:test_id(
                            test_id,
                            title,
                            Questions(
                                question_id,
                                question_text,
                                question_type,
                                Answer_Options(*)
                            )
                        )
                    """)
                    ) {
                        filter { eq("module_id", moduleId) }
                        order("\"order\"", Order.ASCENDING) // "order" в кавычках, т.к. это ключевое слово SQL
                    }
                    .decodeList<JsonObject>()
                    .map { parseModuleContentItem(it) } // Твой парсер теперь получит идеальный JSON

                Log.d("ModuleStepVM", "Успешно загружено: ${content.size} элементов.")
                _uiState.value = ModuleUiState.Success(content)

            } catch (e: Exception) {
                Log.e("ModuleStepVM", "Критическая ошибка при загрузке контента: ", e)
                _uiState.value = ModuleUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun nextStep() {
        val currentState = _uiState.value
        if (currentState is ModuleUiState.Success) {
            if (_currentStepIndex.value < currentState.content.size - 1) {
                _currentStepIndex.value++
            }
        }
    }

    fun previousStep() {
        if (_currentStepIndex.value > 0) {
            _currentStepIndex.value--
        }
    }
}