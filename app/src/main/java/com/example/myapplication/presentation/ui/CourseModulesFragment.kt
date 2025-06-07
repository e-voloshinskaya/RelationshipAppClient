package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import io.github.jan.supabase.postgrest.postgrest
import com.example.myapplication.SupabaseInit
import com.example.myapplication.data.repository.ModuleRepositoryImpl
import com.example.myapplication.databinding.FragmentCourseModulesBinding
import com.example.myapplication.presentation.ModuleViewModel
import com.example.myapplication.presentation.adapters.ModulesAdapter
import com.example.myapplication.presentation.ModulesUiState
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.google.android.material.button.MaterialButton

class CourseModulesFragment : Fragment() {

    // Безопасная работа с ViewBinding во фрагментах
    private var _binding: FragmentCourseModulesBinding? = null
    private val binding get() = _binding!!

    // Создаем адаптер "лениво", чтобы он создался только при первом обращении.
    private val modulesAdapter by lazy {
        ModulesAdapter { module ->
            // 1. Создаем action для перехода, передавая в него ID нажатого модуля.
            //    Имя action (CourseModulesFragmentDirections) генерируется автоматически
            //    библиотекой Navigation Component из твоего nav_graph.xml.
            val action = CourseModulesFragmentDirections.actionCourseModulesFragmentToModuleStepFragment(
                MODULEID = module.id // Передаем ID модуля в аргумент. Имя аргумента (MODULEID) должно совпадать с тем, что в nav_graph.
            )
            // 2. Выполняем переход с помощью NavController'а.
            findNavController().navigate(action)
        }
    }

    // Создаем ViewModel с помощью делегата viewModels и нашей фабрики.
    private val viewModel: ModuleViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val repository = ModuleRepositoryImpl(SupabaseInit.client.postgrest)
                @Suppress("UNCHECKED_CAST")
                return ModuleViewModel(repository) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourseModulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModelState()

        // Обработка системной кнопки "Назад"
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }

        // Находим кнопку "Назад" в верхней панели
        val buttonBack: MaterialButton = binding.modulesHeaderContainer.findViewById(R.id.button_back)

        // Вешаем слушатель клика
        buttonBack.setOnClickListener {
            // Используем NavController, чтобы вернуться назад
            findNavController().navigateUp()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, // связываем с жизненным циклом фрагмента
            callback
        )

    }

    private fun setupRecyclerView() {
        binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(com.example.myapplication.R.id.recycler_view_modules).adapter = modulesAdapter
    }


    private fun observeViewModelState() {
        // Подписываемся на изменения uiState в ViewModel.
        // viewLifecycleOwner гарантирует, что подписка будет активна только
        // пока жив UI фрагмента, что предотвращает утечки памяти.
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // Управляем видимостью элементов в зависимости от текущего состояния
            binding.root.findViewById<android.widget.ProgressBar>(com.example.myapplication.R.id.progress_bar).isVisible = state is ModulesUiState.Loading
            binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(com.example.myapplication.R.id.recycler_view_modules).isVisible = state is ModulesUiState.Success
            binding.root.findViewById<android.widget.TextView>(com.example.myapplication.R.id.text_error).isVisible = state is ModulesUiState.Error

            // Когда данные успешно загружены, передаем их в адаптер
            if (state is ModulesUiState.Success) {
                Log.d("CourseModulesFragment", "Получено модулей: ${state.modules.size}")
                modulesAdapter.submitList(state.modules)
            }

            // Если произошла ошибка, можно показать ее пользователю
            if (state is ModulesUiState.Error) {
                Log.e("CourseModulesFragment", "Ошибка: ${state.message}")
                binding.root.findViewById<android.widget.TextView>(com.example.myapplication.R.id.text_error).text = state.message
            }
        }
    }

    // Крайне важно очищать ссылку на binding в onDestroyView
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}