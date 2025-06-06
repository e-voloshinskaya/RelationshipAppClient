package com.example.myapplication.presentation.ui

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
}