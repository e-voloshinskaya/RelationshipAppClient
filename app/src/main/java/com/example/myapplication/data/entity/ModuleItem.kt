package com.example.myapplication.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Этот enum поможет нам чисто работать со статусами
enum class ModuleStatus {
    IN_PROGRESS,
    COMPLETED,
    NO_STATUS
}

@Serializable
data class ModuleItem(
    // Указываем, что в JSON от Supabase это поле называется "module_id"
    @SerialName("module_id")
    val id: String,

    // А это поле "m_title"
    @SerialName("m_title")
    val title: String,

    @SerialName("num_blocks")
    val sectionsCount: Int,
    // Этих полей нет в таблице Modules. Мы их добавим позже,
    // когда будем обрабатывать данные.
    // @Transient говорит сериализатору игнорировать эти поля.
    @kotlinx.serialization.Transient
    val status: ModuleStatus = ModuleStatus.NO_STATUS, // По умолчанию все "в процессе"
)