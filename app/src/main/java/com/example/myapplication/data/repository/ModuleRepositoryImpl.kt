package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.entity.ModuleItem
import com.example.myapplication.domain.repository.ModuleRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация интерфейса ModuleRepository, использующая Supabase PostgREST для получения данных.
 *
 * @param postgrest Клиент для взаимодействия с базой данных PostgREST в Supabase.
 *                  Передается как зависимость для гибкости и тестируемости.
 */
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
                    .from("Modules") // Имя твоей таблицы в PostgreSQL
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
}

/*
package com.example.myapplication.data.repository

import com.example.myapplication.SupabaseInit
import com.example.myapplication.domain.repository.ModuleRepository
import com.example.myapplication.model.Module
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModuleRepositoryImpl : ModuleRepository {

    override suspend fun getAllModules(): List<Module> = withContext(Dispatchers.IO) {
        val client = SupabaseInit.supabaseClient

        val modulesResponse = client
            .postgrest["Modules"]
            .select()
            .decodeList<Map<String, Any>>() // Или сделай DTO

        // Получаем блоки, чтобы посчитать количество
        val blocksResponse = client
            .postgrest["TheoryBlocks"]
            .select()
            .decodeList<Map<String, Any>>()

        // позже — можно заменить на DTO, если потребуется

        modulesResponse.map { module ->
            val id = module["module_id"] as String
            val title = module["m_title"] as String
            val description = module["description"] as String

            // считаем количество блоков
            val sectionCount = blocksResponse.count {
                it["module_id"] == id
            }

            // Прогресс — временно говорим "завершен", если все блоки есть
            val isCompleted = sectionCount > 0 && Math.random() > 0.5 // TODO: заменить на данные о пользователе

            Module(
                id = id,
                title = title,
                description = description,
                sectionCount = sectionCount,
                isCompleted = isCompleted
            )
        }
    }
}
 */