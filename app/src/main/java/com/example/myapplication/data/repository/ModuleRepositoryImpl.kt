package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.entity.ModuleItem
import com.example.myapplication.data.entity.ModuleStatus
import com.example.myapplication.domain.repository.ModuleRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Реализация интерфейса ModuleRepository, использующая Supabase PostgREST для получения данных.
 *
 * @param postgrest Клиент для взаимодействия с базой данных PostgREST в Supabase.
 *                  Передается как зависимость для гибкости и тестируемости.
 */

@Serializable
private data class ModuleWithStatusDto(
    @SerialName("module_id") val id: String,
    @SerialName("m_title") val title: String,
    @SerialName("sections_count") val sectionsCount: Int,
    @SerialName("status") val statusString: String
)

class ModuleRepositoryImpl(private val postgrest: Postgrest) : ModuleRepository {

    // Ключевое слово 'override' означает, что мы реализуем метод из интерфейса ModuleRepository.
    override suspend fun getModules(): Result<List<ModuleItem>> {

        // Переключаемся на фоновый поток (IO) для выполнения сетевого запроса.
        // Это обязательно, чтобы не блокировать пользовательский интерфейс.
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ModuleRepository", "Запрашиваю модули из Supabase...")

                // Выполняем запрос к таблице "Modules" в Supabase
                val modules = postgrest
                    .from("Modules")
                    .select {
                        // Сортируем результат по колонке "order_idx" в порядке возрастания (ASC)
                        order("order_idx", Order.ASCENDING)
                    }
                    .decodeList<ModuleItem>() // Автоматически преобразуем JSON-ответ в список объектов ModuleItem

                Log.d("ModuleRepository", "Успешно загружено ${modules.size} модулей.")

                // В случае успеха, возвращаем данные, обернутые в Result.success
                Result.success(modules)

            } catch (e: Exception) {
                // Если во время запроса произошла любая ошибка (нет сети, неверное имя таблицы,
                // проблемы с правами доступа RLS в Supabase и т.д.), мы попадаем в этот блок.
                Log.e("ModuleRepository", "Ошибка при загрузке модулей", e)
                e.printStackTrace() // Выводим детали ошибки в лог

                // В случае ошибки, возвращаем исключение, обернутое в Result.failure
                Result.failure(e)
            }
        }
    }

    override suspend fun getModulesWithStatus(): List<ModuleItem> {
        Log.d("ModuleRepository", "Запрашиваю модули со статусом через RPC...")

        val dtos = postgrest.rpc(
            function = "get_modules_with_user_status" // Вызываем нашу новую функцию
        ).decodeList<ModuleWithStatusDto>()

        return dtos.map { dto ->
            val statusEnum = when (dto.statusString) {
                "completed" -> ModuleStatus.COMPLETED
                "in_progress" -> ModuleStatus.IN_PROGRESS
                else -> ModuleStatus.NO_STATUS
            }
            ModuleItem(
                id = dto.id,
                title = dto.title,
                sectionsCount = dto.sectionsCount,
                status = statusEnum
            )
        }
    }
}