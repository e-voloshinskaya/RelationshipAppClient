import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

// Состояния UI для надежной отрисовки во фрагменте
sealed interface ModuleUiState {
    object Loading : ModuleUiState
    data class Success(val content: List<ModuleContentItem>) : ModuleUiState
    data class Error(val message: String) : ModuleUiState
}

// Общий интерфейс для всех элементов контента в модуле (шагов).
sealed interface ModuleContentItem {
    val id: String
    val order: Int
    val type: String
}
// Контейнер для ТЕОРИИ
@Serializable
data class TheoryItem(
    @SerialName("id") override val id: String,
    @SerialName("order") override val order: Int,
    @SerialName("content_type") override val type: String,
    // Ожидаем вложенный объект под ключом "TheoryBlocks"
    @SerialName("TheoryBlocks") val details: TheoryDetails?
) : ModuleContentItem

// Детали ТЕОРИИ
@Serializable
data class TheoryDetails(
    @SerialName("theory_id") val theoryId: String,
    @SerialName("theory_title") val title: String,
    @SerialName("content") val content: String
)

// Контейнер для ТЕСТА
@Serializable
data class TestItem(
    @SerialName("id") override val id: String,
    @SerialName("order") override val order: Int,
    @SerialName("content_type") override val type: String,
    // Ожидаем вложенный объект под ключом "Tests"
    @SerialName("Tests") val details: TestDetails?
) : ModuleContentItem


@Serializable
data class TestDetails(
    @SerialName("test_id") val testId: String,
    @SerialName("title") val title: String,
    @SerialName("Questions") val questions: List<Question> = emptyList()
)

// Модель для одного вопроса в тесте.
@Serializable
data class Question(
    @SerialName("question_id") val questionId: String,
    @SerialName("question_text") val text: String,
    @SerialName("question_type") val questionType: String,
    @SerialName("Answer_Options") val options: List<AnswerOption> = emptyList()
)

// Модель для одного варианта ответа.
@Serializable
data class AnswerOption(
    @SerialName("option_id") val optionId: String,
    @SerialName("option_text") val text: String,
    @SerialName("is_correct") val isCorrect: Boolean
)


// Модель для сохранения прогресса по теории
@Serializable
data class UserTheoryProgress(
    @SerialName("user_id") val userId: String,
    @SerialName("theory_id") val theoryId: String,
    @SerialName("is_completed") val isCompleted: Boolean = true,
    // Время завершения
    @SerialName("completed_at") val completedAt: String = Clock.System.now().toString()
)

// Модель для создания новой попытки теста
@Serializable
data class UserTestAttemptInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("test_id") val testId: String,
    // Время начала
    @SerialName("started_at") val startedAt: String = Clock.System.now().toString()
)

// Модель для получения ID созданной попытки
@Serializable
data class UserTestAttemptResponse(
    @SerialName("attempt_id") val attemptId: String
)

// Модель для отправки ответа пользователя на вопрос
@Serializable
data class UserAnswerInsert(
    @SerialName("attempt_id") var attemptId: String, // `var`, так как мы установим его позже
    @SerialName("question_id") val questionId: String,
    @SerialName("selected_option_id") val selectedOptionId: String? = null,
    @SerialName("free_text_answer") val freeTextAnswer: String? = null
)


private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true         // Позволяет более "свободную" обработку JSON
}

// Используем данный парсер вместо стандартного `Json`
fun parseModuleContentItem(jsonObject: JsonObject): ModuleContentItem {
    return when (val type = jsonObject["content_type"]?.jsonPrimitive?.content) {
        "theory" -> jsonParser.decodeFromJsonElement<TheoryItem>(jsonObject)
        "test" -> jsonParser.decodeFromJsonElement<TestItem>(jsonObject)
        else -> throw IllegalArgumentException("Unknown content type: $type")
    }
}