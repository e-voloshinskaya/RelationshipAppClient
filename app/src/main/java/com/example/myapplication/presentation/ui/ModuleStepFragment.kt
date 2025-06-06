package com.example.myapplication.presentation.ui

// Важно: импортируем новые data-классы и старые для UI
import com.example.myapplication.R // Убедись, что этот импорт правильный
import com.example.myapplication.databinding.FragmentModuleStepBinding
import com.example.myapplication.databinding.ItemTestContainerBinding
import com.example.myapplication.databinding.ItemTheoryBlockBinding
import com.google.android.material.tabs.TabLayoutMediator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

// Импортируем все необходимое из твоего файла с моделями и ViewModel
import ModuleContentItem
import ModuleStepViewModel
import ModuleStepViewModelFactory
import ModuleUiState
import TestItem
import TestsAdapter
import TheoryItem // <- Наш новый класс для теории

class ModuleStepFragment : Fragment() {

    // Используем View Binding для безопасного доступа к элементам UI.
    private var _binding: FragmentModuleStepBinding? = null
    private val binding get() = _binding!!

    // Инициализируем ViewModel с помощью фабрики.
    // Фабрика сама подставит и moduleId, и Supabase клиент.
    private val viewModel: ModuleStepViewModel by viewModels {
        // Когда будешь переходить со списка модулей, раскомментируй этот блок
        /*
        val moduleId = requireArguments().getString("MODULE_ID")
            ?: throw IllegalStateException("Module ID is required for this screen")
        ModuleStepViewModelFactory(moduleId)
        */

        // ВРЕМЕННЫЙ КОД ДЛЯ ТЕСТИРОВАНИЯ: используем "захардкоженный" ID
        val hardcodedModuleId = "c53286b4-38f0-43f7-8afb-f27e178b2bf4"
        ModuleStepViewModelFactory(hardcodedModuleId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализируем биндинг для макета фрагмента
        _binding = FragmentModuleStepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настраиваем все обработчики нажатий
        setupClickListeners()
        // Запускаем наблюдение за состоянием ViewModel
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Навигация по шагам модуля
        binding.buttonNextStep.setOnClickListener { viewModel.nextStep() }
        binding.buttonPreviousStep.setOnClickListener { viewModel.previousStep() }

        // Кнопка "назад" в тулбаре
        binding.toolbar.setNavigationOnClickListener {
            // Этот метод безопасно вернет нас на предыдущий экран
            findNavController().navigateUp()
        }
        // Если ты используешь кастомную кнопку, а не стандартную навигацию тулбара:
        // binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun observeViewModel() {
        // Используем lifecycleScope для безопасного запуска корутин,
        // которые будут жить столько же, сколько и View фрагмента.
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle гарантирует, что сбор данных будет происходить
            // только когда фрагмент находится в состоянии STARTED или выше.
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Первая корутина следит за общим состоянием (загрузка, успех, ошибка)
                launch {
                    viewModel.uiState.collect { state ->
                        // Передаем и состояние, и текущий индекс для отрисовки
                        updateUiForState(state, viewModel.currentStepIndex.value)
                    }
                }

                // Вторая корутина отдельно следит ТОЛЬКО за сменой индекса.
                // Это нужно, чтобы при нажатии "вперед/назад" UI обновлялся мгновенно,
                // даже если данные уже давно загружены и uiState = Success.
                launch {
                    viewModel.currentStepIndex.collect { index ->
                        val currentState = viewModel.uiState.value
                        // Обновляем UI только если данные уже успешно загружены
                        if (currentState is ModuleUiState.Success) {
                            updateUi(currentState.content, index)
                        }
                    }
                }
            }
        }
    }

    /**
     * Главная функция, которая решает, что показать: загрузку, ошибку или контент.
     */
    private fun updateUiForState(state: ModuleUiState, currentIndex: Int) {
        // Скрываем все состояния перед тем, как показать нужное
        binding.progressBar.isVisible = false

        when (state) {
            is ModuleUiState.Loading -> {
                binding.progressBar.isVisible = true
            }
            is ModuleUiState.Success -> {
                // Если данные пришли, вызываем функцию для их отрисовки
                updateUi(state.content, currentIndex)
            }
            is ModuleUiState.Error -> {
                // В случае ошибки показываем ее пользователю
                showError(state.message)
            }

            else -> {}
        }
    }

    /**
     * Эта функция отвечает за отрисовку конкретного шага (теории или теста).
     */
    private fun updateUi(content: List<ModuleContentItem>, currentIndex: Int) {
        if (content.isEmpty() || currentIndex !in content.indices) {
            Log.w("ModuleStepFragment", "Контент пуст или индекс выходит за границы. Нечего отображать.")
            // Можно показать какой-то плейсхолдер, что модуль пуст
            return
        }

        val currentItem = content[currentIndex]
        // Очищаем контейнер перед добавлением нового элемента
        binding.contentContainer.removeAllViews()

        // В зависимости от типа элемента вызываем нужную функцию для отрисовки
        when (currentItem) {
            is TheoryItem -> showTheory(currentItem)
            is TestItem -> showTest(currentItem)
        }

        // Обновляем состояние кнопок навигации
        binding.buttonPreviousStep.isEnabled = currentIndex > 0
        binding.buttonNextStep.isEnabled = currentIndex < content.size - 1

        // Обновляем прогресс-бар
        binding.moduleProgress.progress = ((currentIndex + 1) * 100 / content.size)
    }

    /**
     * Создает и показывает карточку с теорией.
     */
    private fun showTheory(item: TheoryItem) {
        // Создаем View из XML-макета карточки теории
        val theoryBinding = ItemTheoryBlockBinding.inflate(layoutInflater, binding.contentContainer, false)

        // Заполняем View данными из нашей модели
        theoryBinding.tvTheoryTitle.text = item.details?.title ?: "Теория"
        theoryBinding.tvTheoryBody.text = item.details?.content ?: "Нет содержимого."

        // Добавляем созданный View в контейнер на экране
        binding.contentContainer.addView(theoryBinding.root)

        // Обновляем заголовок на тулбаре
        binding.toolbarTitle.text = item.details?.title ?: "Теория"
    }

    /**
     * Создает и показывает карточку с тестом.
     */
    private fun showTest(item: TestItem) {
        // Создаем View из XML-макета контейнера для теста
        val testBinding = ItemTestContainerBinding.inflate(layoutInflater, binding.contentContainer, false)

        // Заполняем заголовок теста
        testBinding.tvTestTitle.text = item.details?.title ?: "Тест"

        // Получаем список вопросов, если он есть
        val questions = item.details?.questions ?: emptyList()
        val testsAdapter = TestsAdapter(questions) // Создаем адаптер для ViewPager2
        testBinding.questionsViewPager.adapter = testsAdapter

        // Связываем ViewPager2 с индикатором-точками (TabLayout)
        TabLayoutMediator(testBinding.questionsTabIndicator, testBinding.questionsViewPager) { _, _ -> }.attach()
        // Скрываем индикатор, если вопрос всего один
        testBinding.questionsTabIndicator.isVisible = questions.size > 1

        // Добавляем созданный View в контейнер на экране
        binding.contentContainer.addView(testBinding.root)

        // Обновляем заголовок на тулбаре
        binding.toolbarTitle.text = item.details?.title ?: "Тест"
    }

    /**
     * Показывает ошибку пользователю.
     */
    private fun showError(message: String) {
        // Самый простой способ — показать Toast
        Toast.makeText(requireContext(), "Ошибка: $message", Toast.LENGTH_LONG).show()
        Log.e("ModuleStepFragment", "Произошла ошибка: $message")
        // Здесь можно добавить более сложную логику, например, показать кнопку "Повторить"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем биндинг, чтобы избежать утечек памяти
        _binding = null
    }
}

/*package com.example.myapplication.presentation.ui

import ModuleContentItem
import ModuleStepViewModel
import ModuleStepViewModelFactory
import ModuleUiState
import TestItem
import TestsAdapter
import TheoryBlock
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.databinding.FragmentModuleStepBinding
import com.example.myapplication.databinding.ItemTestContainerBinding
import com.example.myapplication.databinding.ItemTheoryBlockBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class ModuleStepFragment : Fragment() {

    // Замени на биндинг твоего главного макета (fragment_module_step.xml)
    private lateinit var binding: FragmentModuleStepBinding
    /*
    private val viewModel: ModuleStepViewModel by viewModels {
        // 1. Получаем moduleId из аргументов фрагмента
        // (Это стандартный способ передать ID во фрагмент)
        val moduleId = requireArguments().getString("MODULE_ID")
            ?: throw IllegalStateException("Module ID is required")

        // 2. Используем нашу новую фабрику для создания ViewModel
        ModuleStepViewModelFactory(moduleId)
    } */
    private val viewModel: ModuleStepViewModel by viewModels {
        // val moduleId = requireArguments().getString("MODULE_ID")
        //     ?: throw IllegalStateException("Module ID is required")

        // ВРЕМЕННО ЗАКОММЕНТИРУЙ КОД ВЫШЕ И ВСТАВЬ ID ВРУЧНУЮ
        val hardcodedModuleId = "c53286b4-38f0-43f7-8afb-f27e178b2bf4"

        ModuleStepViewModelFactory(hardcodedModuleId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentModuleStepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonNextStep.setOnClickListener { viewModel.nextStep() }
        binding.buttonPreviousStep.setOnClickListener { viewModel.previousStep() }
        // Добавь обработчик для кнопки "назад" в тулбаре, если нужно
        // binding.appBarLayout.toolbar.setNavigationOnClickListener { ... }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Эта корутина будет следить за общим состоянием (загрузка/успех/ошибка)
                // и за сменой шага, чтобы перерисовать UI.
                viewModel.uiState.collect { state ->
                    // Мы также слушаем currentStepIndex, чтобы получить актуальный индекс
                    updateUiForState(state, viewModel.currentStepIndex.value)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Эта корутина отдельно следит ТОЛЬКО за сменой индекса.
                // Она нужна, чтобы при нажатии "вперед/назад" UI обновлялся мгновенно,
                // даже если данные уже загружены.
                viewModel.currentStepIndex.collect { index ->
                    val currentState = viewModel.uiState.value
                    if (currentState is ModuleUiState.Success) {
                        updateUi(currentState.content, index)
                    }
                }
            }
        }
    }

    private fun updateUiForState(state: ModuleUiState, currentIndex: Int) {
        when (state) {
            is ModuleUiState.Loading -> showLoading(true)
            is ModuleUiState.Success -> {
                showLoading(false)
                updateUi(state.content, currentIndex)
            }
            is ModuleUiState.Error -> {
                showLoading(false)
                showError(state.message)
            }

            else -> {}
        }
    }

    private fun updateUi(content: List<ModuleContentItem>, currentIndex: Int) {
        if (content.isEmpty()) return

        val currentItem = content[currentIndex]
        binding.contentContainer.removeAllViews()

        when (currentItem) {
            is TheoryBlock -> showTheory(currentItem)
            is TestItem -> showTest(currentItem)
            else -> {}
        }

        binding.buttonPreviousStep.isEnabled = currentIndex > 0
        binding.buttonNextStep.isEnabled = currentIndex < content.size - 1

        binding.moduleProgress.progress = ((currentIndex + 1) * 100 / content.size)
    }

    private fun showTheory(item: TheoryBlock) {
        // Замени на биндинг твоей карточки с теорией (content_theory_card.xml)
        val theoryBinding = ItemTheoryBlockBinding.inflate(layoutInflater, binding.contentContainer, false)
        theoryBinding.tvTheoryTitle.text = item.title
        theoryBinding.tvTheoryBody.text = item.content
        binding.contentContainer.addView(theoryBinding.root)
        binding.toolbarTitle.text = "Теория"
    }

    private fun showTest(item: TestItem) {
        // Замени на биндинг твоего контейнера для теста (content_test_container.xml)
        val testBinding = ItemTestContainerBinding.inflate(layoutInflater, binding.contentContainer, false)
        testBinding.tvTestTitle.text = item.details?.title

        val questions = item.details?.questions ?: emptyList()
        val testsAdapter = TestsAdapter(questions) // Адаптер для ViewPager2
        testBinding.questionsViewPager.adapter = testsAdapter

        TabLayoutMediator(testBinding.questionsTabIndicator, testBinding.questionsViewPager) { _, _ -> }.attach()

        binding.contentContainer.addView(testBinding.root)
        binding.toolbarTitle.text = "Тест"
    }

    private fun showLoading(isLoading: Boolean) {
        // Здесь логика показа/скрытия ProgressBar
    }

    private fun showError(message: String) {
        // Здесь логика показа ошибки (например, через Toast или Snackbar)
    }
}*/