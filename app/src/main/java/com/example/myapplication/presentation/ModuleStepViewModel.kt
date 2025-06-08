package com.example.myapplication.presentation

import ModuleContentItem
import ModuleUiState
import TestItem
import TheoryItem
import UserAnswerInsert
import UserTestAttemptInsert
import UserTestAttemptResponse
import UserTheoryProgress
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import parseModuleContentItem

class ModuleStepViewModel(
    private val supabase: SupabaseClient,
    private val moduleId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModuleUiState>(ModuleUiState.Loading)
    val uiState: StateFlow<ModuleUiState> = _uiState

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    // Временное хранилище для ответов на текущий тест
    private val userAnswersCache = mutableMapOf<String, UserAnswerInsert>()
    // Хранит ID ТЕКУЩЕЙ активной попытки прохождения теста
    private var currentAttemptId: String? = null

    private val _moduleFinishedEvent = MutableStateFlow(false)
    val moduleFinishedEvent: StateFlow<Boolean> = _moduleFinishedEvent

    init {
        loadContent()
    }

    fun startTestAttempt(testId: String) {
        // Если попытка для этого сеанса уже создана, ничего не делаем.
        if (currentAttemptId != null) return

        viewModelScope.launch {
            val currentUser = supabase.auth.currentUserOrNull() ?: return@launch
            try {
                // Создаем новую попытку
                val newAttempt = supabase.from("User_TestAttempts").insert(
                    UserTestAttemptInsert(userId = currentUser.id, testId = testId)
                ) { select(Columns.list("attempt_id")) }.decodeSingle<UserTestAttemptResponse>()

                // Сохраняем ID этой попытки в переменную класса
                currentAttemptId = newAttempt.attemptId
                Log.i("ModuleStepVM", "Создана новая попытка прохождения теста: ID = $currentAttemptId")
            } catch (e: Exception) {
                Log.e("ModuleStepVM", "Не удалось создать попытку для теста $testId", e)
            }
        }
    }

    // --- Методы для сбора ответов ---
    fun onOptionSelected(questionId: String, optionId: String) {
        userAnswersCache[questionId] = UserAnswerInsert(attemptId = "", questionId = questionId, selectedOptionId = optionId)
    }

    fun onFreeTextChanged(questionId: String, text: String) {
        if (text.isNotBlank()) {
            userAnswersCache[questionId] = UserAnswerInsert(attemptId = "", questionId = questionId, freeTextAnswer = text)
        } else {
            userAnswersCache.remove(questionId)
        }
    }

    // --- Методы навигации ---
    fun proceedToNextStepOrFinish() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState !is ModuleUiState.Success) return@launch

            // Сохраняем прогресс для текущего шага
            completeCurrentStep(currentState.content)

            // Проверяем, был ли это последний шаг
            if (_currentStepIndex.value >= currentState.content.size - 1) {
                // Если да - отправляем сигнал о завершении
                _moduleFinishedEvent.value = true
            } else {
                // Если нет - просто переходим на следующий шаг
                _currentStepIndex.value++
            }
        }
    }

    fun proceedToPreviousStep() {
        if (_currentStepIndex.value > 0) {
            _currentStepIndex.value--
        }
    }

    // --- Главный метод сохранения ---
    private suspend fun completeCurrentStep(content: List<ModuleContentItem>) {
        val currentItem = content.getOrNull(_currentStepIndex.value) ?: return
        val currentUser = supabase.auth.currentUserOrNull() ?: return

        when (currentItem) {
            is TheoryItem -> {
                val theoryId = currentItem.details?.theoryId ?: return
                try {
                    // Создаем объект со всеми NOT NULL полями
                    val progressData = UserTheoryProgress(
                        userId = currentUser.id,
                        theoryId = theoryId,
                        isCompleted = true // Явно указываем
                    )

                    // Используем upsert, но передаем объект целиком
                    supabase.from("User_Theory_Progress").upsert(progressData)
                    { onConflict = "user_id, theory_id"}
                    Log.i("ModuleStepVM", "Прогресс по теории '$theoryId' успешно сохранен/обновлен.")
                } catch (e: Exception) {
                    Log.e("ModuleStepVM", "Ошибка сохранения теории", e)
                }
            }

            is TestItem -> {
                // Если мы завершаем тест, ID попытки должен уже существовать.
                // Если его нет (произошла ошибка при создании) или нет ответов, выходим.
                val attemptId = currentAttemptId
                if (attemptId == null) {
                    Log.e("ModuleStepVM", "Попытка завершить тест, но ID попытки (attemptId) равен null!")
                    return
                }
                if (userAnswersCache.isEmpty()) {
                    Log.w("ModuleStepVM", "Завершение теста, но нет ответов для сохранения.")
                    // Мы все равно должны завершить попытку
                }

                try {
                    // 1. Сохраняем ответы, если они есть.
                    if (userAnswersCache.isNotEmpty()) {
                        val answersToSubmit = userAnswersCache.values.map { it.copy(attemptId = attemptId) }
                        supabase.from("User_Answers").insert(answersToSubmit)
                        Log.i("ModuleStepVM", "Ответы для попытки '$attemptId' сохранены.")
                    }

                    // 2. Обновляем саму попытку, устанавливая время завершения.
                    supabase.from("User_TestAttempts")
                        .update(buildJsonObject { put("completed_at", Clock.System.now().toString()) }) {
                            filter { eq("attempt_id", attemptId) }
                        }
                    Log.i("ModuleStepVM", "Попытка '$attemptId' отмечена как завершенная.")

                    // 3. Очищаем все для следующего теста.
                    userAnswersCache.clear()
                    currentAttemptId = null

                } catch (e: Exception) {
                    Log.e("ModuleStepVM", "Критическая ошибка при завершении теста и сохранении ответов", e)
                }
            }

            else -> {}
        }
    }

    // --- Загрузка контента и повторная попытка ---
    private fun loadContent() {
        viewModelScope.launch {
            _uiState.value = ModuleUiState.Loading
            try {
                val content = supabase.from("Module_Content")
                    .select(Columns.raw("""
                        id,
                        order,
                        content_type,
                        TheoryBlocks:theory_id(theory_id, theory_title, content),
                        Tests:test_id(test_id, title, Questions(question_id, question_text, question_type, Answer_Options(*)))
                    """)) {
                        filter { eq("module_id", moduleId) }
                        order("\"order\"", Order.ASCENDING)
                    }
                    .decodeList<JsonObject>()
                    .map { parseModuleContentItem(it) }

                _uiState.value = ModuleUiState.Success(content)
            } catch (e: Exception) {
                Log.e("ModuleStepVM", "Критическая ошибка при загрузке контента: ", e)
                _uiState.value = ModuleUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun retryLoadContent() {
        loadContent()
    }
}
