package com.example.myapplication.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.appcompat.widget.PopupMenu
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
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.databinding.FragmentModuleStepBinding
import com.example.myapplication.databinding.ItemTestContainerBinding
import com.example.myapplication.databinding.ItemTheoryBlockBinding
import com.example.myapplication.presentation.adapters.TestsAdapter
import kotlinx.coroutines.launch
import ModuleContentItem
import ModuleStepViewModel
import ModuleStepViewModelFactory
import ModuleUiState
import TestItem
import TheoryItem
import com.example.myapplication.R

class ModuleStepFragment : Fragment() {

    // Используем View Binding для безопасного доступа к элементам UI.
    private var _binding: FragmentModuleStepBinding? = null
    private val binding get() = _binding!!

    // Ссылка на ViewPager, чтобы мы могли управлять им из разных частей фрагмента.
    private var testViewPager: ViewPager2? = null

    // Инициализируем ViewModel с помощью фабрики.
    private val viewModel: ModuleStepViewModel by viewModels {
        // 1. Получаем moduleId из аргументов, которые нам передал CourseModulesFragment
        val moduleId = requireArguments().getString("MODULE_ID")
            ?: throw IllegalStateException("Module ID is required for this screen")

        // 2. Используем нашу фабрику для создания ViewModel с реальным ID
        ModuleStepViewModelFactory(moduleId)

        // ВРЕМЕННЫЙ КОД ДЛЯ ТЕСТИРОВАНИЯ: используем "захардкоженный" ID
        //val hardcodedModuleId = "c53286b4-38f0-43f7-8afb-f27e178b2bf4"
        //ModuleStepViewModelFactory(hardcodedModuleId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModuleStepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonNextStep.setOnClickListener {
            val testPager = testViewPager
            if (testPager != null && testPager.currentItem < (testPager.adapter?.itemCount ?: 0) - 1) {
                // Если на экране тест и это не последний вопрос, листаем вопрос вперед.
                testPager.currentItem += 1
            } else {
                // Если это теория или последний вопрос теста, переходим на следующий шаг модуля.
                viewModel.nextStep()
            }
        }

        binding.buttonPreviousStep.setOnClickListener {
            val testPager = testViewPager
            if (testPager != null && testPager.currentItem > 0) {
                // Если на экране тест и это не первый вопрос, листаем вопрос назад.
                testPager.currentItem -= 1
            } else {
                // Если это теория или первый вопрос теста, переходим на предыдущий шаг модуля.
                viewModel.previousStep()
            }
        }

        // Обработчик для кнопки "назад" в тулбаре
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // --- НАЧАЛО ИЗМЕНЕНИЙ: ОБРАБОТЧИК ДЛЯ НОВОЙ КНОПКИ "ТРИ ТОЧКИ" ---
        binding.buttonMoreOptions.setOnClickListener { view ->
            showOptionsMenu(view)
        }
    }

    private fun showOptionsMenu(anchorView: View) {
        // Создаем PopupMenu, привязанное к контексту фрагмента и "якорю" (нашей кнопке)
        val popupMenu = PopupMenu(requireContext(), anchorView)

        // "Надуваем" наше меню из XML-файла
        popupMenu.menuInflater.inflate(R.menu.module_step_menu, popupMenu.menu)

        // Устанавливаем обработчик нажатий на пункты этого меню
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // ID из нашего обновленного menu.xml
                R.id.action_report_problem -> {
                    val url = "https://docs.google.com/forms/d/e/1FAIpQLScMaHPzEd_pnbDq8XMBOQCyDHfzOlkRic2y_E6ATSbTmK4JHg/viewform?usp=preview"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_exit_lesson -> {
                    findNavController().navigateUp()
                    true
                }
                // Заглушки для новых кнопок
                R.id.action_add_bookmark, R.id.action_share -> {
                    Toast.makeText(requireContext(), "${menuItem.title} в разработке", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // Включаем показ иконок (по умолчанию они могут быть скрыты)
        // Этот трюк использует рефлексию, но он безопасен и широко используется
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popupMenu)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            Log.e("ModuleStepFragment", "Error showing menu icons.", e)
        }

        // Показываем меню
        popupMenu.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Эта корутина следит за общим состоянием и сменой ШАГА МОДУЛЯ
                launch {
                    viewModel.uiState.collect { state ->
                        updateUiForState(state, viewModel.currentStepIndex.value)
                    }
                }
                // Эта корутина отдельно следит за сменой индекса ШАГА МОДУЛЯ,
                // чтобы UI мгновенно обновлялся при вызове viewModel.nextStep() / previousStep()
                launch {
                    viewModel.currentStepIndex.collect { index ->
                        val currentState = viewModel.uiState.value
                        if (currentState is ModuleUiState.Success) {
                            updateUi(currentState.content, index)
                        }
                    }
                }
            }
        }
    }

    private fun updateUiForState(state: ModuleUiState, currentIndex: Int) {
        // Показываем/скрываем ProgressBar
        binding.progressBar.isVisible = state is ModuleUiState.Loading

        when (state) {
            is ModuleUiState.Success -> {
                // Если данные пришли, вызываем функцию для их отрисовки
                updateUi(state.content, currentIndex)
            }
            is ModuleUiState.Error -> {
                // В случае ошибки показываем ее пользователю
                showError(state.message)
            }
            else -> { /* Для Loading больше ничего делать не нужно */ }
        }
    }

    private fun updateUi(content: List<ModuleContentItem>, currentIndex: Int) {
        if (content.isEmpty() || currentIndex !in content.indices) {
            Log.w("ModuleStepFragment", "Контент пуст или индекс выходит за границы.")
            return
        }

        // --- ИЗМЕНЕНИЕ 2: СБРОС VIEWPAGER ПЕРЕД ОТРИСОВКОЙ НОВОГО ШАГА ---
        testViewPager = null

        val currentItem = content[currentIndex]
        binding.contentContainer.removeAllViews()

        when (currentItem) {
            is TheoryItem -> showTheory(currentItem)
            is TestItem -> showTest(currentItem)
            else -> {}
        }

        // --- ИЗМЕНЕНИЕ 3: ВЫЗОВ НОВОЙ ФУНКЦИИ ДЛЯ ОБНОВЛЕНИЯ КНОПОК ---
        updateMainNavigationButtons()

        // Обновляем общий прогресс-бар модуля
        binding.moduleProgress.progress = ((currentIndex + 1) * 100 / content.size)
    }

    // --- ИЗМЕНЕНИЕ 4: НОВАЯ ФУНКЦИЯ ДЛЯ УПРАВЛЕНИЯ ГЛАВНЫМИ КНОПКАМИ ---
    private fun updateMainNavigationButtons() {
        val currentStepIndex = viewModel.currentStepIndex.value
        val totalSteps = (viewModel.uiState.value as? ModuleUiState.Success)?.content?.size ?: 0

        val testPager = testViewPager
        if (testPager != null) {
            // Если на экране тест, состояние кнопок зависит и от шага, и от вопроса
            val currentQuestionIndex = testPager.currentItem
            val totalQuestions = testPager.adapter?.itemCount ?: 0

            binding.buttonPreviousStep.isEnabled = currentStepIndex > 0 || currentQuestionIndex > 0
            binding.buttonNextStep.isEnabled = currentStepIndex < totalSteps - 1 || currentQuestionIndex < totalQuestions - 1
        } else {
            // Если на экране теория, все просто
            binding.buttonPreviousStep.isEnabled = currentStepIndex > 0
            binding.buttonNextStep.isEnabled = currentStepIndex < totalSteps - 1
        }
    }

    private fun showTheory(item: TheoryItem) {
        val theoryBinding = ItemTheoryBlockBinding.inflate(layoutInflater, binding.contentContainer, false)
        theoryBinding.tvTheoryTitle.text = item.details?.title ?: "Теория"
        theoryBinding.tvTheoryBody.text = item.details?.content ?: "Нет содержимого."
        binding.contentContainer.addView(theoryBinding.root)

        val currentStep = viewModel.currentStepIndex.value + 1
        val totalSteps = (viewModel.uiState.value as? ModuleUiState.Success)?.content?.size ?: 1
        binding.toolbarTitle.text = "Раздел $currentStep из $totalSteps"
    }

    // --- ИЗМЕНЕНИЕ 5: ПОЛНОСТЬЮ ПЕРЕРАБОТАННАЯ ФУНКЦИЯ SHOWTEST ---
    private fun showTest(item: TestItem) {
        val testBinding = ItemTestContainerBinding.inflate(layoutInflater, binding.contentContainer, false)
        testBinding.tvTestTitle.text = item.details?.title ?: "Тест"

        val questions = item.details?.questions ?: emptyList()
        val testsAdapter = TestsAdapter(questions)

        // Сохраняем ссылку на ViewPager в переменную класса
        testViewPager = testBinding.questionsViewPager
        testViewPager?.adapter = testsAdapter

        // Функция для обновления текстового индикатора "Вопрос X из Y"
        fun updateQuestionProgress(position: Int) {
            val total = questions.size
            if (total > 1) {
                testBinding.tvQuestionProgress.isVisible = true
                testBinding.tvQuestionProgress.text = "Вопрос ${position + 1} из $total"
            } else {
                testBinding.tvQuestionProgress.isVisible = false
            }
        }

        // Слушаем смену страниц, чтобы обновлять индикатор и главные кнопки
        testViewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateQuestionProgress(position)
                updateMainNavigationButtons()
            }
        })

        // Инициализируем индикатор для первой страницы
        updateQuestionProgress(0)

        binding.contentContainer.addView(testBinding.root)

        val currentStep = viewModel.currentStepIndex.value + 1
        val totalSteps = (viewModel.uiState.value as? ModuleUiState.Success)?.content?.size ?: 1
        binding.toolbarTitle.text = "Раздел $currentStep из $totalSteps"
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), "Ошибка: $message", Toast.LENGTH_LONG).show()
        Log.e("ModuleStepFragment", "Произошла ошибка: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем ViewPager и биндинг, чтобы избежать утечек памяти
        testViewPager = null
        _binding = null
    }
}