package com.example.myapplication.presentation.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.myapplication.SupabaseInit
import com.example.myapplication.data.repository.PairingCodeRepositoryImpl
import com.example.myapplication.databinding.FragmentPairingBinding
import com.example.myapplication.domain.repository.PairingCodeRepository
import com.example.myapplication.domain.repository.PairingResult
import com.example.myapplication.domain.repository.PairingUiState
import com.example.myapplication.presentation.PairingViewModel

class PairingFragment : Fragment() {

    private var _binding: FragmentPairingBinding? = null
    private val binding get() = _binding!!
    // Здесь хранится текущий код пользователя для кнопок "Копировать" и "Поделиться"
    private var currentUserCode: String? = null

    private val viewModel: PairingViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val repository: PairingCodeRepository = PairingCodeRepositoryImpl(SupabaseInit.client)
                @Suppress("UNCHECKED_CAST")
                return PairingViewModel(repository) as T
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPairingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupClickListeners()
    }

    // В файле PairingFragment.kt

    private fun observeViewModel() {
        // 1. Подписываемся на ЕДИНОЕ состояние UI
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // Сначала делаем ВСЕ контейнеры невидимыми.
            // Это гарантирует, что на экране не останется "мусора" от предыдущего состояния.
            Log.d("PairingFragment", "Получено новое состояние UI: $state")
            binding.progressBar.isVisible = false
            binding.noLinkContent.isVisible = false
            binding.requestSentContent.isVisible = false
            binding.requestReceivedContent.isVisible = false
            binding.linkedContent.isVisible = false

            // Теперь показываем ТОЛЬКО ОДИН нужный контейнер
            when (state) {
                is PairingUiState.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is PairingUiState.NoLink -> {
                    binding.noLinkContent.isVisible = true
                    binding.yourCodeText.text = state.userCode.replace("-", " - ")
                    currentUserCode = state.userCode
                }
                is PairingUiState.RequestSent -> {
                    binding.requestSentContent.isVisible = true
                    binding.requestSentText.text = "Приглашение отправлено пользователю ${state.partnerName ?: "..."}. Ожидание ответа."
                }
                is PairingUiState.RequestReceived -> {
                    binding.requestReceivedContent.isVisible = true
                    binding.requestReceivedText.text = "${state.partnerName ?: "Пользователь"} хочет создать с вами пару."
                }
                is PairingUiState.Linked -> {
                    binding.linkedContent.isVisible = true
                    binding.partnerNameText.text = state.partnerName ?: "Ваш партнер"
                }
                is PairingUiState.Error -> {
                    // Если ошибка, можно показать основной экран с кодом, но с Toast-ом
                    binding.noLinkContent.isVisible = true
                    Toast.makeText(requireContext(), "Ошибка: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 2. Подписка на результат связывания (остается без изменений)
        viewModel.pairingResultEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { result ->
                when (result) {
                    is PairingResult.Success -> {
                        Toast.makeText(requireContext(), "Успех! Обновляем статус...", Toast.LENGTH_SHORT).show()
                    }
                    is PairingResult.Error -> {
                        binding.partnerCodeInputLayout.error = result.message
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.copyButton.setOnClickListener {
            currentUserCode?.let { code ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Pairing Code", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Код скопирован!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.shareButton.setOnClickListener {
            currentUserCode?.let { code ->
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Привет! Присоединяйся ко мне в приложении. Мой код для связи: $code")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
        }

        // "Оживляем" кнопку "Pair now"
        binding.pairNowButton.setOnClickListener {
            val code = binding.partnerCodeInputEditText.text.toString()
            viewModel.onPairNowClicked(code)
        }

        // Убираем ошибку, когда пользователь начинает вводить текст
        binding.partnerCodeInputEditText.doAfterTextChanged {
            binding.partnerCodeInputLayout.error = null
        }

        binding.closeButton.setOnClickListener {
            // Просто возвращаемся на предыдущий экран
            findNavController().navigateUp()
        }

        // Кнопки для новых состояний
        binding.acceptRequestButton.setOnClickListener { viewModel.onAcceptRequest() }
        binding.declineRequestButton.setOnClickListener { viewModel.onDeclineRequest() }
        binding.cancelRequestButton.setOnClickListener { viewModel.onCancelRequest() }
        binding.unlinkButton.setOnClickListener { viewModel.onUnlink() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}