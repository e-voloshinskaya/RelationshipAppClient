package com.example.myapplication.data.repository

import HistoryItem
import android.util.Log
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// --- ИНТЕРФЕЙС РЕПОЗИТОРИЯ ---
interface HistoryRepository {
    suspend fun getUserHistory(): List<HistoryItem>
}


// --- РЕАЛИЗАЦИЯ РЕПОЗИТОРИЯ ---
class HistoryRepositoryImpl(private val postgrest: Postgrest) : HistoryRepository {

    // DTO для получения "сырых" данных из Supabase
    @Serializable
    private data class HistoryItemDto(
        @SerialName("attempt_id") val attemptId: String,
        @SerialName("test_id") val testId: String,
        @SerialName("test_title") val testTitle: String,
        @SerialName("is_pair_activity") val isPairActivity: Boolean,
        @SerialName("user_completed_at") val userCompletedAt: String?,
        @SerialName("partner_id") val partnerId: String?,
        @SerialName("partner_completed_at") val partnerCompletedAt: String?
    )

    override suspend fun getUserHistory(): List<HistoryItem> {
        return try {
            val dtos = postgrest.rpc(
                function = "get_user_history"
            ).decodeList<HistoryItemDto>()

            // Конвертируем DTO в UI-модель, с которой будет работать адаптер
            dtos.mapNotNull { dto ->
                // Отфильтровываем попытки, которые почему-то не завершены.
                val userCompletedDate = dto.userCompletedAt ?: return@mapNotNull null

                // Статус текущего пользователя всегда "пройден" для записей в истории.
                val userStatus = CompletionStatus.COMPLETED

                // Определяем статус партнера.
                val partnerStatus = when {
                    !dto.isPairActivity -> CompletionStatus.NOT_APPLICABLE
                    dto.partnerCompletedAt != null -> CompletionStatus.COMPLETED
                    else -> CompletionStatus.NOT_COMPLETED
                }

                HistoryItem(
                    attemptId = dto.attemptId,
                    testId = dto.testId,
                    title = dto.testTitle,
                    // Форматируем дату в красивый вид
                    completedAt = formatIsoDate(userCompletedDate),
                    userStatus = userStatus,
                    partnerStatus = partnerStatus
                )
            }
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error fetching user history", e)
            emptyList() // Возвращаем пустой список в случае ошибки
        }
    }

    // Вспомогательная функция для форматирования даты
    private fun formatIsoDate(isoDate: String): String {
        return try {
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
            isoFormatter.timeZone = TimeZone.getTimeZone("UTC")
            val date = isoFormatter.parse(isoDate)

            val outputFormatter = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru"))
            date?.let { outputFormatter.format(it) } ?: isoDate
        } catch (e: Exception) {
            isoDate // В случае ошибки вернем исходную строку
        }
    }
}