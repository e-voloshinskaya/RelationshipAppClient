package com.example.myapplication.presentation

import HistoryItem
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.HistoryRepository
import kotlinx.coroutines.launch

// Состояния для UI, чтобы фрагмент знал, что показывать
sealed interface HistoryUiState {
    object Loading : HistoryUiState
    data class Success(val history: List<HistoryItem>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<HistoryUiState>(HistoryUiState.Loading)
    val uiState: LiveData<HistoryUiState> = _uiState

    init {
        loadHistory()
    }

    fun loadHistory() {
        // Устанавливаем состояние "Загрузка" перед запросом
        _uiState.value = HistoryUiState.Loading

        viewModelScope.launch {
            try {
                // Получаем данные из репозитория
                val historyList = repository.getUserHistory()
                // Устанавливаем состояние "Успех" с полученными данными
                _uiState.postValue(HistoryUiState.Success(historyList))
                Log.d("HistoryViewModel", "История успешно загружена: ${historyList.size} элементов.")
            } catch (e: Exception) {
                // В случае любой ошибки устанавливаем состояние "Ошибка"
                _uiState.postValue(HistoryUiState.Error(e.message ?: "Неизвестная ошибка"))
                Log.e("HistoryViewModel", "Ошибка при загрузке истории", e)
            }
        }
    }
}