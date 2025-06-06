import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Общий интерфейс для всех элементов контента в модуле (шагов).
 */
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

// Контейнер для ТЕСТА (остается как есть)
@Serializable
data class TestItem(
    @SerialName("id") override val id: String,
    @SerialName("order") override val order: Int,
    @SerialName("content_type") override val type: String,
    // Ожидаем вложенный объект под ключом "Tests"
    @SerialName("Tests") val details: TestDetails?
) : ModuleContentItem


/**
 * Модель для блока ТЕОРИИ.
 * Она напрямую реализует интерфейс, т.к. не имеет вложенных подразделов.

@Serializable
data class TheoryBlock(
    // Поля из таблицы Module_Content
    @SerialName("id") override val id: String,
    @SerialName("order") override val order: Int,
    @SerialName("content_type") override val type: String,

    // Поля из связанной таблицы TheoryBlocks
    @SerialName("theory_id") val theoryId: String,
    @SerialName("theory_title") val title: String,
    @SerialName("content") val content: String

) : ModuleContentItem

/**
 * Модель для элемента "ТЕСТ".
 * Является контейнером для деталей теста.
 */
@Serializable
data class TestItem(
    // Поля из таблицы Module_Content
    @SerialName("id") override val id: String,
    @SerialName("order") override val order: Int,
    @SerialName("content_type") override val type: String,

    // Вложенный объект с деталями из таблицы Tests
    @SerialName("Tests") val details: TestDetails?

) : ModuleContentItem
*/
/**
 * Детали теста, содержащие список вопросов.
 */
@Serializable
data class TestDetails(
    @SerialName("test_id") val testId: String,
    @SerialName("title") val title: String,
    @SerialName("Questions") val questions: List<Question> = emptyList()
)

/**
 * Модель для одного вопроса в тесте.
 */
@Serializable
data class Question(
    @SerialName("question_id") val questionId: String,
    @SerialName("question_text") val text: String,
    @SerialName("question_type") val questionType: String,
    @SerialName("Answer_Options") val options: List<AnswerOption> = emptyList()
)

/**
 * Модель для одного варианта ответа.
 */
@Serializable
data class AnswerOption(
    @SerialName("option_id") val optionId: String,
    @SerialName("option_text") val text: String,
    @SerialName("is_correct") val isCorrect: Boolean
)

/**
 * Главная функция-парсер, которая преобразует JsonObject от Supabase в наши data-классы.

fun parseModuleContentItem(jsonObject: JsonObject): ModuleContentItem {
return when (val type = jsonObject["content_type"]?.jsonPrimitive?.content) {
"theory" -> {
// "Расплющиваем" два JSON-объекта (корневой и вложенный) в один для парсера
val theoryDetails = jsonObject["TheoryBlocks"] as JsonObject
val mergedJson = JsonObject(jsonObject + theoryDetails)
Json.decodeFromJsonElement<TheoryBlock>(mergedJson)
}
"test" -> {
Json.decodeFromJsonElement<TestItem>(jsonObject)
}
else -> throw IllegalArgumentException("Unknown content type: $type")
}
}

fun parseModuleContentItem(jsonObject: JsonObject): ModuleContentItem {
    // kotlinx.serialization делает всю работу за нас, без ручного "расплющивания"
    return when (val type = jsonObject["content_type"]?.jsonPrimitive?.content) {
        "theory" -> Json.decodeFromJsonElement<TheoryItem>(jsonObject)
        "test" -> Json.decodeFromJsonElement<TestItem>(jsonObject)
        else -> throw IllegalArgumentException("Unknown content type: $type")
    }
}
 */
private val jsonParser = Json {
    ignoreUnknownKeys = true // <-- ВОЛШЕБНАЯ СТРОЧКА!
    isLenient = true         // Позволяет более "свободную" обработку JSON
}

// 2. Используем НАШ парсер вместо стандартного `Json`
fun parseModuleContentItem(jsonObject: JsonObject): ModuleContentItem {
    return when (val type = jsonObject["content_type"]?.jsonPrimitive?.content) {
        "theory" -> jsonParser.decodeFromJsonElement<TheoryItem>(jsonObject)
        "test" -> jsonParser.decodeFromJsonElement<TestItem>(jsonObject)
        else -> throw IllegalArgumentException("Unknown content type: $type")
    }
}

