package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.domain.repository.PairingCodeRepository
import com.example.myapplication.domain.repository.PairingResult
import com.example.myapplication.domain.repository.PairingUiState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

// Data-класс для работы с таблицей PairingCodes
@Serializable
private data class PairingCode(
    @SerialName("code_id") val codeId: String? = null,
    val code: String,
    @SerialName("user_id") val userId: String,
    @SerialName("expires_at") val expiresAt: String
)

class PairingCodeRepositoryImpl(
    private val client: SupabaseClient
) : PairingCodeRepository {

    override suspend fun getOrCreateCode(): String {
        val currentUser = client.auth.currentUserOrNull() ?: throw IllegalStateException("Пользователь не авторизован")
        val userId = currentUser.id

        // 1. Ищем существующий активный код
        val now = Clock.System.now()
        val existingCodes = client.postgrest.from("PairingCodes")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("is_used", false)
                    gt("expires_at", now.toString())
                }
            }.decodeList<PairingCode>()

        // Если есть активный код, и он не истекает в ближайшие 5 минут - возвращаем его
        if (existingCodes.isNotEmpty()) {
            val latestCode = existingCodes.first()
            val expirationInstant = Instant.parse(latestCode.expiresAt)
            if (expirationInstant > (now + 5.minutes)) {
                Log.d("PairingRepo", "Найден существующий активный код: ${latestCode.code}")
                return latestCode.code
            }
        }

        // 2. Если активного кода нет или он скоро истечет, генерируем новый
        Log.d("PairingRepo", "Активный код не найден или скоро истечет. Генерируем новый...")
        val newCode = generateUniqueCode(userId)
        val newExpiration = (now + 30.minutes).toString()

        // 3. Сохраняем новый код в базу
        client.postgrest.from("PairingCodes").insert(
            PairingCode(
                code = newCode,
                userId = userId,
                expiresAt = newExpiration
            )
        )
        Log.d("PairingRepo", "Новый код '$newCode' сохранен в БД.")
        return newCode
    }

    override suspend fun usePairingCode(code: String): PairingResult {
        // Эту логику реализуем на следующем шаге
        TODO("Not yet implemented")
    }

    // Приватные методы для генерации кода
    private suspend fun generateUniqueCode(userId: String): String {
        // Пытаемся сгенерировать код, например, 10 раз.
        repeat(10) {
            val code = generateUserPrefix(userId) + "-" + generateRandomSuffix()

            // --- ИСПРАВЛЕНИЕ ЗДЕСЬ ---
            // Мы просто запрашиваем один столбец. Нам не важны данные, только факт их наличия.
            val result = client.postgrest.from("PairingCodes")
                .select(columns = Columns.list("code_id")) { // Выбираем любую колонку, например, code_id
                    filter {
                        eq("code", code)
                        eq("is_used", false)
                        gt("expires_at", Clock.System.now().toString())
                    }
                    limit(1) // Важно: нам достаточно найти хотя бы одну запись, чтобы понять, что код не уникален
                }.decodeList<PairingCode>() // Декодируем в список

            // Если список пуст, значит, такого активного кода не найдено, и он уникален
            if (result.isEmpty()) {
                Log.d("PairingRepo", "Сгенерирован уникальный код: $code")
                return code
            }
        }

        // Если за 10 попыток не удалось, выбрасываем ошибку.
        throw IllegalStateException("Не удалось сгенерировать уникальный код после 10 попыток.")
    }

    private fun generateUserPrefix(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(userId.toByteArray())
        val hex = hash.joinToString("") { String.format(Locale.US, "%02x", it) }
        return hex.take(3).uppercase(Locale.US)
    }

    private fun generateRandomSuffix(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..3).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    override suspend fun acceptLinkRequest(): Result<Unit> = try {
        client.postgrest.rpc("accept_link_request")
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteLink(): Result<Unit> = try {
        client.postgrest.rpc("delete_link")
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    @Serializable
    private data class PairingStatusDto(
        @SerialName("user_status") val status: String,
        @SerialName("partner_id") val partnerId: String? = null,
        @SerialName("partner_name") val partnerName: String? = null,
        @SerialName("partner_avatar") val partnerAvatar: String? = null
    )

    // РЕАЛИЗАЦИЯ НЕДОСТАЮЩЕЙ ФУНКЦИИ
    override suspend fun getPairingStatus(): PairingUiState {
        return try {
            // Вызываем SQL-функцию, которую мы создали ранее
            val dto = client.postgrest.rpc("get_user_pairing_status")
                .decodeSingle<PairingStatusDto>()

            // В зависимости от ответа от сервера, возвращаем нужный объект состояния
            when (dto.status) {
                "no_link" -> PairingUiState.NoLink(getOrCreateCode()) // Если нет связи, заодно получаем код
                "request_sent" -> PairingUiState.RequestSent(dto.partnerName)
                "request_received" -> PairingUiState.RequestReceived(dto.partnerName)
                "linked" -> PairingUiState.Linked(dto.partnerName, dto.partnerAvatar)
                else -> PairingUiState.Error("Неизвестный статус от сервера")
            }
        } catch (e: Exception) {
            Log.e("PairingRepo", "Ошибка при получении статуса пары", e)
            PairingUiState.Error(e.message ?: "Ошибка загрузки статуса")
        }
    }
}