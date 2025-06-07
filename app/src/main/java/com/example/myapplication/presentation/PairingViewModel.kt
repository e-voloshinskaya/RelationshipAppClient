package com.example.myapplication.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.repository.PairingCodeRepository
import com.example.myapplication.domain.repository.PairingResult
import com.example.myapplication.domain.repository.PairingUiState
import kotlinx.coroutines.launch

// Вспомогательный класс для одноразовых событий (чтобы Toast не показывался снова при повороте экрана)
open class Event<out T>(private val content: T) {
    private var hasBeenHandled = false
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}

class PairingViewModel(
    private val repository: PairingCodeRepository
) : ViewModel() {

    // LiveData для хранения текущего состояния UI (Загрузка, Одиночка, В паре и т.д.)
    private val _uiState = MutableLiveData<PairingUiState>(PairingUiState.Loading)
    val uiState: LiveData<PairingUiState> = _uiState

    // LiveData для одноразовых событий, таких как результат попытки связывания
    private val _pairingResultEvent = MutableLiveData<Event<PairingResult>>()
    val pairingResultEvent: LiveData<Event<PairingResult>> = _pairingResultEvent

    init {
        // Загружаем статус сразу при создании ViewModel
        fetchStatus()
    }

    /**
     * Главная функция для получения текущего статуса пользователя.
     * Вызывается при инициализации и после каждого действия (принять, отклонить и т.д.).
     */
    fun fetchStatus() {
        // Сообщаем UI, что началась загрузка
        _uiState.value = PairingUiState.Loading
        viewModelScope.launch {
            try {
                // Запрашиваем статус из репозитория
                val status = repository.getPairingStatus()
                // Отправляем результат в UI
                _uiState.postValue(status)
                Log.d("PairingViewModel", "Статус успешно загружен: $status")
            } catch (e: Exception) {
                // В случае ошибки отправляем в UI состояние ошибки
                _uiState.postValue(PairingUiState.Error(e.message ?: "Неизвестная ошибка"))
                Log.e("PairingViewModel", "Ошибка при загрузке статуса", e)
            }
        }
    }

    /**
     * Вызывается, когда пользователь нажимает кнопку "Создать пару".
     */
    fun onPairNowClicked(partnerCode: String) {
        if (partnerCode.isBlank()) {
            _pairingResultEvent.value = Event(PairingResult.Error("Введите код партнера."))
            return
        }

        viewModelScope.launch {
            // Отправляем результат как одноразовое событие
            val result = repository.usePairingCode(partnerCode.uppercase())
            _pairingResultEvent.postValue(Event(result))

            // Если связывание прошло успешно, перезагружаем статус, чтобы UI обновился
            if (result is PairingResult.Success) {
                fetchStatus()
            }
        }
    }

    /**
     * Вызывается при нажатии на кнопку "Принять"
     */
    fun onAcceptRequest() = viewModelScope.launch {
        repository.acceptLinkRequest().onSuccess { fetchStatus() }
    }

    /**
     * Вызывается при нажатии на кнопку "Отклонить"
     */
    fun onDeclineRequest() = viewModelScope.launch {
        repository.deleteLink().onSuccess { fetchStatus() }
    }

    /**
     * Вызывается при нажатии на кнопку "Отменить запрос"
     */
    fun onCancelRequest() = viewModelScope.launch {
        repository.deleteLink().onSuccess { fetchStatus() }
    }

    /**
     * Вызывается при нажатии на кнопку "Разорвать связь"
     */
    fun onUnlink() = viewModelScope.launch {
        repository.deleteLink().onSuccess { fetchStatus() }
    }
}