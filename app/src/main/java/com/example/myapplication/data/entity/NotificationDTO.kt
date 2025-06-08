package com.example.myapplication.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val message: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("sender_id") val senderId: String,
    val type: String,
    @SerialName("related_item_id") val relatedItemId: String? = null
)

// Enum для типов уведомлений
enum class NotificationType(val value: String) {
    TEST_COMPLETED("test_completed"),
    MODULE_COMPLETED("module_completed"),
    LOVEMAP_UPDATED("lovemap_updated"),
    DIARY_ENTRY("diary_entry"),
    GENERAL("general");

    companion object {
        fun fromValue(value: String): NotificationType {
            return entries.find { it.value == value } ?: GENERAL
        }
    }
}