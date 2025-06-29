import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.SupabaseInit
import com.example.myapplication.presentation.ModuleStepViewModel

/**
 * Эта фабрика - как инструкция по сборке для Android.
 * Она говорит: "Чтобы создать com.example.myapplication.presentation.ModuleStepViewModel, возьми moduleId,
 * а supabaseClient возьми из SupabaseInit.client".
 */
class ModuleStepViewModelFactory(
    private val moduleId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Проверяем, что система просит у нас именно com.example.myapplication.presentation.ModuleStepViewModel
        if (modelClass.isAssignableFrom(ModuleStepViewModel::class.java)) {
            // Создаем и возвращаем экземпляр ViewModel
            return ModuleStepViewModel(
                supabase = SupabaseInit.client,
                // moduleId передаем тот, что получили в конструкторе фабрики
                moduleId = moduleId
            ) as T
        }
        // Если просят какой-то другой ViewModel, выбрасываем ошибку
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}