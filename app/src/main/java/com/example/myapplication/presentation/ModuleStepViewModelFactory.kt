import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.SupabaseInit

/**
 * Эта фабрика - как инструкция по сборке для Android.
 * Она говорит: "Чтобы создать ModuleStepViewModel, возьми moduleId,
 * а supabaseClient возьми из SupabaseInit.client".
 */
class ModuleStepViewModelFactory(
    private val moduleId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Проверяем, что система просит у нас именно ModuleStepViewModel
        if (modelClass.isAssignableFrom(ModuleStepViewModel::class.java)) {
            // Создаем и возвращаем экземпляр ViewModel
            return ModuleStepViewModel(
                // Клиент берем из твоего синглтона
                supabase = SupabaseInit.client,
                // А moduleId передаем тот, что получили в конструкторе фабрики
                moduleId = moduleId
            ) as T
        }
        // Если просят какой-то другой ViewModel, выбрасываем ошибку
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}