package com.example.myapplication.data.repository

import com.example.myapplication.SupabaseInit
import com.example.myapplication.data.entity.NotificationDto
import com.example.myapplication.presentation.models.AppNotification
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class NotificationRepositoryImpl {
    private val client = SupabaseInit.client
    private val json = Json { ignoreUnknownKeys = true }

    // Канал для realtime подписки
    private var notificationChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    // Подписка на новые уведомления через Realtime
    suspend fun subscribeToNotifications(scope: CoroutineScope): Flow<NotificationDto> {
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User not authenticated")

        // Создаем канал
        val channel = client.channel("notifications-channel")
        notificationChannel = channel

        // Создаем flow для INSERT событий БЕЗ фильтра
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "notifications"
            // Убираем filter отсюда
        }

        // Подписываемся на канал
        channel.subscribe()

        // Фильтруем на клиенте
        return flow.map { action ->
            // Декодируем JsonObject в NotificationDto
            json.decodeFromString<NotificationDto>(action.record.toString())
        }.filter { notification ->
            // Проверяем, что уведомление предназначено текущему пользователю
            notification.recipientId == userId
        }
    }

    // Отписка от канала
    suspend fun unsubscribeFromNotifications() {
        notificationChannel?.unsubscribe()
        notificationChannel = null
    }

    // Получить все уведомления пользователя
    suspend fun getAllNotifications(): List<AppNotification> {
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User not authenticated")

        val response = client.postgrest
            .from("notifications")
            .select() {
                filter {
                    eq("recipient_id", userId)
                }
                order(column = "created_at", order = Order.ASCENDING)
            }

        val notifications = response.decodeList<NotificationDto>()

        return notifications.map { it.toAppNotification() }
    }

    // Получить количество непрочитанных уведомлений
    suspend fun getUnreadCount(): Int {
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User not authenticated")

        val response = client.postgrest
            .from("notifications")
            .select(columns = Columns.list("id")) {
                filter {
                    eq("recipient_id", userId)
                    eq("is_read", false)
                }
            }

        val notifications = response.decodeList<NotificationDto>()
        return notifications.size
    }

    // Пометить все уведомления как прочитанные
    suspend fun markAllAsRead() {
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User not authenticated")

        client.postgrest
            .from("notifications")
            .update(
                {
                    set("is_read", true)
                }
            ) {
                filter {
                    eq("recipient_id", userId)
                    eq("is_read", false)
                }
            }
    }

    // Пометить конкретное уведомление как прочитанное
    suspend fun markAsRead(notificationId: String) {
        client.postgrest
            .from("notifications")
            .update(
                {
                    set("is_read", true)
                }
            ) {
                filter {
                    eq("id", notificationId)
                }
            }
    }

    // Расширение для конвертации DTO в модель для UI
    private fun NotificationDto.toAppNotification(): AppNotification {
        return try {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val parsedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(createdAt)

            AppNotification(
                id = id,
                message = message,
                isRead = isRead,
                date = dateFormat.format(parsedDate ?: Date()),
                type = type // Добавляем type
            )
        } catch (e: Exception) {
            AppNotification(
                id = id,
                message = message,
                isRead = isRead,
                date = createdAt,
                type = type // Добавляем type
            )
        }
    }
}