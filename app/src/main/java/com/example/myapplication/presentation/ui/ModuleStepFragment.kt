package com.example.myapplication.presentation.ui

import android.os.Bundle
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
import ModuleStepViewModelFactory
import ModuleUiState
import TestItem
import TheoryItem
import com.example.myapplication.presentation.ModuleStepViewModel

class ModuleStepFragment : Fragment() {

    private var _binding: FragmentModuleStepBinding? = null
    private val binding get() = _binding!!

    private var testViewPager: ViewPager2? = null

    private val viewModel: ModuleStepViewModel by viewModels {
        val moduleId = requireArguments().getString("MODULE_ID")
            ?: throw IllegalStateException("Module ID is required")
        ModuleStepViewModelFactory(moduleId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentModuleStepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        val nextClickListener = View.OnClickListener {
            val testPager = testViewPager
            if (testPager != null && testPager.currentItem < (testPager.adapter?.itemCount ?: 0) - 1) {
                testPager.currentItem += 1
            } else {
                viewModel.proceedToNextStepOrFinish()
            }
        }
        binding.buttonNextStepDefault.setOnClickListener(nextClickListener)
        binding.buttonNextStepAnswer.setOnClickListener(nextClickListener)

        binding.buttonPreviousStep.setOnClickListener {
            val testPager = testViewPager
            if (testPager != null && testPager.currentItem > 0) {
                testPager.currentItem -= 1
            } else {
                viewModel.proceedToPreviousStep()
            }
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        // binding.buttonMoreOptions.setOnClickListener { showOptionsMenu(it) } // Раскомментируй, когда добавишь меню
        binding.emptyStateRetryButton.setOnClickListener { viewModel.retryLoadContent() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUiForState(state, viewModel.currentStepIndex.value)
                    }
                }
                launch {
                    viewModel.currentStepIndex.collect { index ->
                        if (viewModel.uiState.value is ModuleUiState.Success) {
                            updateUi((viewModel.uiState.value as ModuleUiState.Success).content, index)
                        }
                    }
                }
                launch {
                    viewModel.moduleFinishedEvent.collect { isFinished ->
                        if (isFinished) {
                            Toast.makeText(requireContext(), "Модуль пройден!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }

    private fun updateUiForState(state: ModuleUiState, currentIndex: Int) {
        binding.progressBar.isVisible = state is ModuleUiState.Loading
        binding.emptyStateContainer.isVisible = false
        binding.scrollView.isVisible = true
        binding.navigationPanel.isVisible = true
        binding.appBarLayout.isVisible = true

        when (state) {
            is ModuleUiState.Loading -> {
                binding.scrollView.isVisible = false
                binding.navigationPanel.isVisible = false
            }
            is ModuleUiState.Success -> {
                updateUi(state.content, currentIndex)
            }
            is ModuleUiState.Error -> {
                showEmptyState("Ошибка загрузки", state.message, showRetryButton = true)
            }

            else -> {}
        }
    }

    private fun showEmptyState(title: String, message: String, showRetryButton: Boolean) {
        binding.appBarLayout.isVisible = false
        binding.scrollView.isVisible = false
        binding.navigationPanel.isVisible = false
        binding.emptyStateContainer.isVisible = true
        binding.emptyStateTitle.text = title
        binding.emptyStateMessage.text = message
        binding.emptyStateRetryButton.isVisible = showRetryButton
    }

    private fun updateUi(content: List<ModuleContentItem>, currentIndex: Int) {
        if (content.isEmpty()) {
            showEmptyState("Урок пуст", "В этом разделе пока нет материалов.", showRetryButton = false)
            return
        }
        if (currentIndex !in content.indices) return

        testViewPager = null
        binding.contentContainer.removeAllViews()
        when (val currentItem = content[currentIndex]) {
            is TheoryItem -> showTheory(currentItem)
            is TestItem -> showTest(currentItem)
            else -> {}
        }
        updateMainNavigationButtons()
        val totalSteps = content.size
        binding.moduleProgress.progress = ((currentIndex + 1) * 100 / totalSteps)
        binding.toolbarTitle.text = "Раздел ${currentIndex + 1} из $totalSteps"
    }

    private fun updateMainNavigationButtons() {
        val currentStepIndex = viewModel.currentStepIndex.value
        val testPager = testViewPager
        binding.buttonPreviousStep.isEnabled = currentStepIndex > 0 || (testPager != null && testPager.currentItem > 0)

        if (testPager != null) {
            binding.buttonNextStepDefault.isVisible = false
            binding.buttonNextStepAnswer.isVisible = true
        } else {
            binding.buttonNextStepDefault.isVisible = true
            binding.buttonNextStepAnswer.isVisible = false
        }
    }

    private fun showTheory(item: TheoryItem) {
        val theoryBinding = ItemTheoryBlockBinding.inflate(layoutInflater, binding.contentContainer, false)
        theoryBinding.tvTheoryTitle.text = item.details?.title
        theoryBinding.tvTheoryBody.text = item.details?.content
        binding.contentContainer.addView(theoryBinding.root)
    }

    private fun showTest(item: TestItem) {
        item.details?.testId?.let { viewModel.startTestAttempt(it) }
        val testBinding = ItemTestContainerBinding.inflate(layoutInflater, binding.contentContainer, false)
        testBinding.tvTestTitle.text = item.details?.title
        val questions = item.details?.questions ?: emptyList()
        val testsAdapter = TestsAdapter(
            questions,
            onOptionSelected = { qId, oId -> viewModel.onOptionSelected(qId, oId) },
            onFreeTextChanged = { qId, text -> viewModel.onFreeTextChanged(qId, text) }
        )
        testViewPager = testBinding.questionsViewPager
        testViewPager?.adapter = testsAdapter
        fun updateQuestionProgress(position: Int) {
            val total = questions.size
            testBinding.tvQuestionProgress.isVisible = total > 1
            if (total > 1) testBinding.tvQuestionProgress.text = "Вопрос ${position + 1} из $total"
        }
        testViewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateQuestionProgress(position)
                updateMainNavigationButtons()
            }
        })
        updateQuestionProgress(0)
        binding.contentContainer.addView(testBinding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        testViewPager = null
        _binding = null
    }
}
/*package com.example.myapplication.presentation.ui

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
import com.example.myapplication.presentation.ModuleStepViewModel
import ModuleStepViewModelFactory
import ModuleUiState
import TestItem
import TheoryItem

class ModuleStepFragment : Fragment() {

    private var _binding: FragmentModuleStepBinding? = null
    private val binding get() = _binding!!

    private var testViewPager: ViewPager2? = null

    private val viewModel: ModuleStepViewModel by viewModels {
        val moduleId = requireArguments().getString("MODULE_ID")
            ?: throw IllegalStateException("Module ID is required for this screen")
        ModuleStepViewModelFactory(moduleId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentModuleStepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        val nextClickListener = View.OnClickListener {
            val testPager = testViewPager
            if (testPager != null && testPager.currentItem < (testPager.adapter?.itemCount ?: 0) - 1) {
                testPager.currentItem += 1
            } else {
                viewModel.nextStep()
            }
        }
        binding.buttonNextStepDefault.setOnClickListener(nextClickListener)
        binding.buttonNextStepAnswer.setOnClickListener(nextClickListener)

        binding.buttonPreviousStep.setOnClickListener {
            val testPager = testViewPager
            if (testPager != null && testPager.currentItem > 0) {
                testPager.currentItem -= 1
            } else {
                viewModel.previousStep()
            }
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.buttonMoreOptions.setOnClickListener { showOptionsMenu(it) }
        binding.emptyStateRetryButton.setOnClickListener { viewModel.retryLoadContent() }
    }

    private fun showOptionsMenu(anchorView: View) {
        // ... твой код для показа PopupMenu ...
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUiForState(state, viewModel.currentStepIndex.value)
                    }
                }
                launch {
                    viewModel.currentStepIndex.collect { index ->
                        if (viewModel.uiState.value is ModuleUiState.Success) {
                            updateUi((viewModel.uiState.value as ModuleUiState.Success).content, index)
                        }
                    }
                }
                launch {
                    viewModel.moduleFinishedEvent.collect { isFinished ->
                        if (isFinished) {
                            Toast.makeText(requireContext(), "Модуль пройден!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }

    private fun updateUiForState(state: ModuleUiState, currentIndex: Int) {
        binding.progressBar.isVisible = state is ModuleUiState.Loading
        binding.emptyStateContainer.isVisible = false
        binding.scrollView.isVisible = true
        binding.navigationPanel.isVisible = true

        when (state) {
            is ModuleUiState.Loading -> {
                binding.scrollView.isVisible = false
                binding.navigationPanel.isVisible = false
            }
            is ModuleUiState.Success -> {
                updateUi(state.content, currentIndex)
            }
            is ModuleUiState.Error -> {
                showEmptyState("Ошибка загрузки", state.message, showRetryButton = true)
            }
        }
    }

    private fun showEmptyState(title: String, message: String, showRetryButton: Boolean) {
        binding.emptyStateContainer.isVisible = true
        binding.scrollView.isVisible = false
        binding.navigationPanel.isVisible = false
        binding.emptyStateTitle.text = title
        binding.emptyStateMessage.text = message
        binding.emptyStateRetryButton.isVisible = showRetryButton
    }

    private fun updateUi(content: List<ModuleContentItem>, currentIndex: Int) {
        if (content.isEmpty()) {
            showEmptyState("Урок пуст", "В этом разделе пока нет материалов.", showRetryButton = false)
            binding.appBarLayout.isVisible = false
            return
        }
        binding.appBarLayout.isVisible = true

        testViewPager = null
        binding.contentContainer.removeAllViews()
        val currentItem = content[currentIndex]
        when (currentItem) {
            is TheoryItem -> showTheory(currentItem)
            is TestItem -> showTest(currentItem)
        }
        updateMainNavigationButtons()
        val totalSteps = content.size
        binding.moduleProgress.progress = ((currentIndex + 1) * 100 / totalSteps)
        binding.toolbarTitle.text = "Раздел ${currentIndex + 1} из $totalSteps"
    }

    private fun updateMainNavigationButtons() {
        val currentStepIndex = viewModel.currentStepIndex.value
        val testPager = testViewPager
        binding.buttonPreviousStep.isEnabled = currentStepIndex > 0 || (testPager != null && testPager.currentItem > 0)

        if (testPager != null) {
            binding.buttonNextStepDefault.isVisible = false
            binding.buttonNextStepAnswer.isVisible = true
        } else {
            binding.buttonNextStepDefault.isVisible = true
            binding.buttonNextStepAnswer.isVisible = false
        }
    }

    private fun showTheory(item: TheoryItem) {
        val theoryBinding = ItemTheoryBlockBinding.inflate(layoutInflater, binding.contentContainer, false)
        theoryBinding.tvTheoryTitle.text = item.details?.title
        theoryBinding.tvTheoryBody.text = item.details?.content
        binding.contentContainer.addView(theoryBinding.root)
    }

    private fun showTest(item: TestItem) {
        val testBinding = ItemTestContainerBinding.inflate(layoutInflater, binding.contentContainer, false)
        testBinding.tvTestTitle.text = item.details?.title
        val questions = item.details?.questions ?: emptyList()
        val testsAdapter = TestsAdapter(
            questions,
            onOptionSelected = { qId, oId -> viewModel.onOptionSelected(qId, oId) },
            onFreeTextChanged = { qId, text -> viewModel.onFreeTextChanged(qId, text) }
        )
        testViewPager = testBinding.questionsViewPager
        testViewPager?.adapter = testsAdapter
        fun updateQuestionProgress(position: Int) {
            val total = questions.size
            testBinding.tvQuestionProgress.isVisible = total > 1
            if (total > 1) testBinding.tvQuestionProgress.text = "Вопрос ${position + 1} из $total"
        }
        testViewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateQuestionProgress(position)
                updateMainNavigationButtons()
            }
        })
        updateQuestionProgress(0)
        binding.contentContainer.addView(testBinding.root)
    }

    private fun showError(message: String) {
        // Ошибку теперь показывает showEmptyState, но лог можно оставить
        Log.e("ModuleStepFragment", "Произошла ошибка: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        testViewPager = null
        _binding = null
    }
}
*/
/*package com.example.myapplication.presentation.ui

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
import com.example.myapplication.presentation.ModuleStepViewModel
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
    private val viewModel: com.example.myapplication.presentation.ModuleStepViewModel by viewModels {
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

        binding.emptyStateRetryButton.setOnClickListener {
            // Просто просим ViewModel перезагрузить контент
            viewModel.retryLoadContent()
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

    private fun showEmptyState(title: String, message: String, showRetryButton: Boolean) {
        // Делаем видимым наш контейнер-заглушку
        binding.emptyStateContainer.isVisible = true

        // Скрываем основной контент
        binding.scrollView.isVisible = false
        binding.navigationPanel.isVisible = false

        // Настраиваем текст и видимость кнопки
        binding.emptyStateTitle.text = title
        binding.emptyStateMessage.text = message
        binding.emptyStateRetryButton.isVisible = showRetryButton
    }

    private fun updateUiForState(state: ModuleUiState, currentIndex: Int) {
        // Сначала скрываем все "особенные" состояния
        binding.progressBar.isVisible = false
        binding.emptyStateContainer.isVisible = false
        // И показываем основной контент (его может скрыть showEmptyState)
        binding.scrollView.isVisible = true
        binding.navigationPanel.isVisible = true

        when (state) {
            is ModuleUiState.Loading -> {
                binding.progressBar.isVisible = true
                // Во время загрузки скрываем основной контент
                binding.scrollView.isVisible = false
                binding.navigationPanel.isVisible = false
            }
            is ModuleUiState.Success -> {
                updateUi(state.content, currentIndex)
            }
            is ModuleUiState.Error -> {
                // В случае ошибки показываем нашу заглушку с кнопкой "Повторить"
                showEmptyState(
                    title = "Ошибка загрузки",
                    message = state.message,
                    showRetryButton = true
                )
            }

            else -> {}
        }
    }

    private fun updateUi(content: List<ModuleContentItem>, currentIndex: Int) {
        if (content.isEmpty()) {
            // Если список контента пуст, показываем заглушку без кнопки "Повторить"
            showEmptyState(
                title = "Урок пуст",
                message = "В этом разделе пока нет материалов.",
                showRetryButton = false
            )
            // Скрываем тулбар, т.к. нет разделов для отображения
            binding.appBarLayout.isVisible = false
            return
        }

        // Если контент есть, показываем тулбар
        binding.appBarLayout.isVisible = true

        if (content.isEmpty() || currentIndex !in content.indices) {
            Log.w("ModuleStepFragment", "Контент пуст или индекс выходит за границы.")
            return
        }

        // СБРОС VIEWPAGER ПЕРЕД ОТРИСОВКОЙ НОВОГО ШАГА
        testViewPager = null

        val currentItem = content[currentIndex]
        binding.contentContainer.removeAllViews()

        when (currentItem) {
            is TheoryItem -> showTheory(currentItem)
            is TestItem -> showTest(currentItem)
            else -> {}
        }

        // ВЫЗОВ НОВОЙ ФУНКЦИИ ДЛЯ ОБНОВЛЕНИЯ КНОПОК
        updateMainNavigationButtons()

        // Обновляем общий прогресс-бар модуля
        binding.moduleProgress.progress = ((currentIndex + 1) * 100 / content.size)
    }

    // --- ФУНКЦИЯ ДЛЯ УПРАВЛЕНИЯ ГЛАВНЫМИ КНОПКАМИ ---
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

 */