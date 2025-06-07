package com.example.myapplication.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.entity.ModuleItem
import com.example.myapplication.domain.repository.ModuleRepository
import kotlinx.coroutines.launch

/**
 * Sealed-класс для представления всех возможных состояний UI экрана модулей.
 * Это позволяет обрабатывать состояния (загрузка, успех, ошибка) в одном месте
 * и делает код фрагмента очень чистым и безопасным.
 */
sealed class ModulesUiState {
    object Loading : ModulesUiState()
    data class Success(val modules: List<ModuleItem>) : ModulesUiState()
    data class Error(val message: String) : ModulesUiState()
}

/**
 * ViewModel для экрана списка модулей.
 *
 * @param moduleRepository Репозиторий для получения данных о модулях.
 *                         Мы передаем его как зависимость, чтобы ViewModel не знала,
 *                         откуда берутся данные, что делает ее тестируемой.
 */
class ModuleViewModel(
    private val moduleRepository: ModuleRepository
) : ViewModel() {

    // Внутренняя, изменяемая LiveData. Только ViewModel может менять ее значение.
    private val _uiState = MutableLiveData<ModulesUiState>()

    // Внешняя, публичная, неизменяемая LiveData. Фрагмент будет на нее подписываться.
    val uiState: LiveData<ModulesUiState> = _uiState

    // Блок init выполняется один раз при создании ViewModel.
    // Мы сразу же запускаем загрузку модулей.
    init {
        loadModulesWithStatus() // Вызываем новый метод при инициализации
    }


    // НОВЫЙ МЕТОД ЗАГРУЗКИ
    private fun loadModulesWithStatus() {
        _uiState.value = ModulesUiState.Loading
        viewModelScope.launch {
            try {
                // Вызываем новый метод репозитория
                val modules = moduleRepository.getModulesWithStatus()
                _uiState.postValue(ModulesUiState.Success(modules))
                Log.d("ModuleViewModel", "Успешно загружены модули со статусами: ${modules.size} шт.")
            } catch (e: Exception) {
                _uiState.postValue(ModulesUiState.Error(e.message ?: "Ошибка загрузки"))
                Log.e("ModuleViewModel", "Ошибка при загрузке модулей со статусами", e)
            }
        }
    }
    /**
     * Запускает процесс загрузки модулей из репозитория.
     * Эту функцию можно будет позже вызывать для обновления списка (например, pull-to-refresh).
     */
    fun loadModules() {
        // Устанавливаем состояние "Загрузка", чтобы UI мог показать ProgressBar.
        _uiState.value = ModulesUiState.Loading

        // Запускаем корутину в viewModelScope. Эта корутина автоматически
        // отменится, когда ViewModel будет уничтожена, что предотвращает утечки памяти.
        viewModelScope.launch {
            // Вызываем suspend-функцию репозитория.
            val result = moduleRepository.getModules()

            // Обрабатываем результат с помощью удобных функций onSuccess и onFailure.
            result.onSuccess { modules ->
                // Данные успешно получены. Обновляем состояние на Success.
                // Используем postValue, так как мы находимся в фоновом потоке корутины.
                _uiState.postValue(ModulesUiState.Success(modules))
            }.onFailure { error ->
                // Произошла ошибка. Обновляем состояние на Error.
                _uiState.postValue(ModulesUiState.Error(error.message ?: "Неизвестная ошибка"))
            }
        }
    }
}