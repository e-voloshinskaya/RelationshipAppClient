package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.SupabaseInit
import com.example.myapplication.data.repository.HistoryRepository
import com.example.myapplication.data.repository.HistoryRepositoryImpl
import com.example.myapplication.databinding.FragmentHistoryBinding
import com.example.myapplication.presentation.HistoryUiState
import com.example.myapplication.presentation.HistoryViewModel
import com.example.myapplication.presentation.adapters.HistoryAdapter
import io.github.jan.supabase.postgrest.postgrest

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val historyAdapter by lazy {
        HistoryAdapter { historyItem ->
            Toast.makeText(requireContext(), "Нажата история: ${historyItem.title}", Toast.LENGTH_SHORT).show()
        }
    }

    // Создаем ViewModel с помощью фабрики
    private val viewModel: HistoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val repository: HistoryRepository = HistoryRepositoryImpl(SupabaseInit.client.postgrest)
                @Suppress("UNCHECKED_CAST")
                return HistoryViewModel(repository) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupClickListeners() {
        // ЗАГЛУШКИ для кнопок в шапке
        binding.iconNotes.setOnClickListener {
            Toast.makeText(requireContext(), "Заметки в разработке", Toast.LENGTH_SHORT).show()
        }
        binding.iconBookmarks.setOnClickListener {
            Toast.makeText(requireContext(), "Закладки в разработке", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // --- ИСПРАВЛЕННАЯ ЛОГИКА ---
        // Подписываемся на `uiState` из ViewModel, который мы создали
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // Сначала скрываем все состояния
            binding.progressBar.isVisible = false
            binding.recyclerViewHistory.isVisible = false
            binding.emptyStateContainer.isVisible = false

            // Теперь показываем нужное состояние
            when (state) {
                is HistoryUiState.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is HistoryUiState.Success -> {
                    val historyList = state.history
                    Log.d("HistoryFragment", "Получено ${historyList.size} элементов истории")
                    if (historyList.isEmpty()) {
                        // Если список пуст, показываем заглушку "История пока пуста"
                        binding.emptyStateContainer.isVisible = true
                        binding.emptyStateTitle.text = "История пока пуста"
                        binding.emptyStateMessage.text = "Проходите уроки, чтобы увидеть здесь результаты."
                    } else {
                        // Если есть данные, показываем список
                        binding.recyclerViewHistory.isVisible = true
                        historyAdapter.submitList(historyList)
                    }
                }
                is HistoryUiState.Error -> {
                    // Если ошибка, показываем заглушку с текстом ошибки
                    binding.emptyStateContainer.isVisible = true
                    binding.emptyStateTitle.text = "Ошибка"
                    binding.emptyStateMessage.text = state.message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}