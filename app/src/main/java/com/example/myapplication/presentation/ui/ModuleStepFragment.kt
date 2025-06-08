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
        // binding.buttonMoreOptions.setOnClickListener { showOptionsMenu(it) }
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