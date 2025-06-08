package com.example.myapplication.domain.repository

// Результат попытки связывания
sealed interface PairingResult {
    object Success : PairingResult
    data class Error(val message: String) : PairingResult
}

// Описывает все возможные состояния экрана PairingFragment
sealed interface PairingUiState {
    object Loading : PairingUiState
    data class NoLink(val userCode: String) : PairingUiState
    data class RequestSent(val partnerName: String?) : PairingUiState
    data class RequestReceived(val partnerName: String?) : PairingUiState
    data class Linked(val partnerName: String?, val partnerAvatar: String?) : PairingUiState
    data class Error(val message: String) : PairingUiState
}

interface PairingCodeRepository {
    // Получает существующий активный код или создает новый
    suspend fun getOrCreateCode(): String

    // Использует код партнера для создания связи
    suspend fun usePairingCode(code: String): PairingResult

    // Методы для управления связью
    suspend fun acceptLinkRequest(): Result<Unit>
    suspend fun deleteLink(): Result<Unit>

    suspend fun getPairingStatus(): PairingUiState
}