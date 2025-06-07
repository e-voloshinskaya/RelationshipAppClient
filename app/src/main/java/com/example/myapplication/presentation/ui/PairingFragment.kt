package com.example.myapplication.presentation.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    private fun observeViewModel() {
        // 1. Подписываемся на ЕДИНОЕ состояние UI
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // Сначала все скрываем, чтобы избежать наложения состояний
            binding.progressBar.isVisible = false
            binding.contentContainer.isVisible = false // Скрываем основной ScrollView с карточками
            // ... скрой здесь другие контейнеры, если ты их создавала ...
            // binding.requestSentContent.isVisible = false
            // binding.requestReceivedContent.isVisible = false
            // binding.linkedContent.isVisible = false

            // Теперь показываем нужное состояние
            when (state) {
                is PairingUiState.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is PairingUiState.NoLink -> {
                    // Показываем основной контейнер с двумя карточками
                    binding.contentContainer.isVisible = true
                    binding.yourCodeText.text = state.userCode
                    currentUserCode = state.userCode
                }
                is PairingUiState.RequestSent -> {
                    // TODO: Показать контейнер "Запрос отправлен"
                    // binding.requestSentContent.isVisible = true
                    // binding.requestSentText.text = "Ожидание ответа от ${state.partnerName ?: "..."}"
                }
                is PairingUiState.RequestReceived -> {
                    // TODO: Показать контейнер "Запрос получен"
                    // binding.requestReceivedContent.isVisible = true
                    // binding.requestReceivedText.text = "${state.partnerName ?: "..."} хочет создать пару"
                }
                is PairingUiState.Linked -> {
                    // TODO: Показать контейнер "В паре"
                    // binding.linkedContent.isVisible = true
                    // binding.partnerNameText.text = state.partnerName
                }
                is PairingUiState.Error -> {
                    // TODO: Показать заглушку с ошибкой
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // 2. Подписываемся на ОДНОРАЗОВОЕ событие результата связывания
        viewModel.pairingResultEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { result ->
                // Этот код сработает только один раз после нажатия "Создать пару"
                when (result) {
                    is PairingResult.Success -> {
                        Toast.makeText(requireContext(), "Успех! Обновляем статус...", Toast.LENGTH_SHORT).show()
                        // UI обновится сам, так как ViewModel вызовет fetchStatus()
                    }
                    is PairingResult.Error -> {
                        // Показываем ошибку в поле ввода
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