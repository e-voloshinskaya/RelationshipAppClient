package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.util.Log // Добавь для логов
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
// import androidx.navigation.fragment.findNavController // УБИРАЕМ, если навигация только из MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAuthBinding
import com.example.myapplication.presentation.AuthUiState
import com.example.myapplication.presentation.AuthViewModel

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AuthFragment" // Для логов

    // Предполагается, что AuthViewModel инжектируется через Hilt/Koin или создается фабрикой
// Если нет, убедись, что он правильно создается.
    private val viewModel: AuthViewModel by viewModels() // Убедись, что AuthViewModel правильно настроен

    private var isSignInMode = true // Начнем с режима входа по умолчанию

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        setupClickListeners()
        observeUiState()
        updateUI() // Вызываем для установки начального состояния UI
    }

    private fun setupClickListeners() {
        binding.buttonPrimary.setOnClickListener {
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            it.clearFocus()

            // Передаем текущее значение isSignInMode из фрагмента в ViewModel
            if (isSignInMode) {
                Log.d(TAG, "Попытка входа: $email")
                viewModel.signIn(email, password, isSignInMode) // ИСПРАВЛЕНО: добавлен параметр
            } else {
                Log.d(TAG, "Попытка регистрации: $email")
                viewModel.signUp(email, password, isSignInMode) // ИСПРАВЛЕНО: добавлен параметр
            }
        }

        binding.buttonSwitchMode.setOnClickListener {
            isSignInMode = !isSignInMode
            updateUI()
            clearErrors()
            // Если нужно сбрасывать состояние ViewModel при смене режима:
            // viewModel.resetUiStateToIdle() // (этот метод нужно будет добавить в AuthViewModel)
        }
    }


    private fun updateUI() {
        if (isSignInMode) {
            binding.inputLayoutPassword.hint = "Пароль"// Может быть полезно
            binding.textTitle.text = "Вход в приложение"
            binding.buttonPrimary.text = "Войти"
            binding.textSwitchMode.text = "Нет аккаунта? "
            binding.buttonSwitchMode.text = "Зарегистрироваться"
        } else {
            binding.textTitle.text = "Регистрация"
            binding.buttonPrimary.text = "Создать аккаунт"
            binding.textSwitchMode.text = "Уже есть аккаунт? "
            binding.buttonSwitchMode.text = "Войти"
            binding.inputLayoutPassword.hint = "Пароль" // Убедись, что подсказки подходят
        }
        // Сбрасываем текст в полях при смене режима, если нужно
        // binding.inputEmail.text?.clear()
        // binding.inputPassword.text?.clear()
    }

    private fun clearErrors() {
        binding.inputLayoutEmail.error = null
        binding.inputLayoutPassword.error = null
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "Новое состояние UI: $state")
            when (state) {
                is AuthUiState.Idle -> {
                    showLoading(false)
                }
                is AuthUiState.Loading -> {
                    showLoading(true)
                    clearErrors()
                }
                is AuthUiState.Success -> {
                    showLoading(false)
                    // --- КЛЮЧЕВОЕ ИЗМЕНЕНИЕ ---
                    // НЕ ВЫПОЛНЯЕМ НАВИГАЦИЮ ОТСЮДА
                    // findNavController().navigate(R.id.action_authFragment_to_main_graph) // ЭТУ СТРОКУ УДАЛЯЕМ
                    Log.i(TAG, "AuthUiState.Success получен. Ожидаем навигацию из MainActivity.")
                    // Если это был signUp, то просто ждем подтверждения по email.
                    // Если это был signIn, MainActivity должна среагировать на SessionStatus.Authenticated.
                    // Можно показать сообщение об успехе, если это signUp.
                    if (!isSignInMode && state.successMessage != null) { // Если была регистрация и есть сообщение
                        Toast.makeText(requireContext(), state.successMessage, Toast.LENGTH_LONG).show()
                    } else if (isSignInMode && state.successMessage != null) {
                        // Для signIn успех означает, что MainActivity должна перенаправить.
                        // Сообщение здесь может быть излишним, если переход быстрый.
                        Log.i(TAG, "Успешный вход, MainActivity должна перенаправить.")
                    }
                }
                is AuthUiState.Error -> {
                    showLoading(false)
                    showError(state.errorMessage)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.buttonPrimary.isEnabled = !isLoading
        // Можно также блокировать поля ввода и кнопку переключения режима
        binding.inputEmail.isEnabled = !isLoading
        binding.inputPassword.isEnabled = !isLoading
        binding.buttonSwitchMode.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        // Улучшенная логика показа ошибок (можно и дальше развивать)
        if (message.contains("email", ignoreCase = true) || message.contains("почта", ignoreCase = true)) {
            binding.inputLayoutEmail.error = message
            binding.inputLayoutPassword.error = null // Сбрасываем ошибку с другого поля
        } else if (message.contains("password", ignoreCase = true) || message.contains("пароль", ignoreCase = true)) {
            binding.inputLayoutPassword.error = message
            binding.inputLayoutEmail.error = null // Сбрасываем ошибку с другого поля
        } else {
            // Общая ошибка, не привязанная к конкретному полю
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
        Log.e(TAG, "Ошибка UI: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView")
    }
}
/*
package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.util.Log // Добавь для логов
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
// import androidx.navigation.fragment.findNavController // УБИРАЕМ, если навигация только из MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAuthBinding
import com.example.myapplication.presentation.AuthUiState
import com.example.myapplication.presentation.AuthViewModel

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AuthFragment" // Для логов

    private val viewModel: AuthViewModel by viewModels()

    private var isSignInMode = false // Начнем с режима регистрации по умолчанию

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        setupClickListeners()
        observeUiState()
        updateUI() // Вызываем для установки начального состояния UI
    }

    private fun setupClickListeners() {
        binding.buttonPrimary.setOnClickListener {
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            it.clearFocus()

            // Передаем текущее значение isSignInMode из фрагмента в ViewModel
            if (isSignInMode) {
                Log.d(TAG, "Попытка входа: $email")
                viewModel.signIn(email, password, isSignInMode) // ИСПРАВЛЕНО: добавлен параметр
            } else {
                Log.d(TAG, "Попытка регистрации: $email")
                viewModel.signUp(email, password, isSignInMode) // ИСПРАВЛЕНО: добавлен параметр
            }
        }

        binding.buttonSwitchMode.setOnClickListener {
            isSignInMode = !isSignInMode
            updateUI()
            clearErrors()
            // Если нужно сбрасывать состояние ViewModel при смене режима:
            // viewModel.resetUiStateToIdle() // (этот метод нужно будет добавить в AuthViewModel)
        }
    }


    private fun updateUI() {
        if (isSignInMode) {
            binding.textTitle.text = "Вход в приложение"
            binding.buttonPrimary.text = "Войти"
            binding.textSwitchMode.text = "Нет аккаунта? "
            binding.buttonSwitchMode.text = "Зарегистрироваться"
        } else {
            binding.textTitle.text = "Регистрация"
            binding.buttonPrimary.text = "Создать аккаунт"
            binding.textSwitchMode.text = "Уже есть аккаунт? "
            binding.buttonSwitchMode.text = "Войти"
        }
        // Сбрасываем текст в полях при смене режима, если нужно
        // binding.inputEmail.text?.clear()
        // binding.inputPassword.text?.clear()
    }

    private fun clearErrors() {
        binding.inputLayoutEmail.error = null
        binding.inputLayoutPassword.error = null
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "Новое состояние UI: $state")
            when (state) {
                is AuthUiState.Idle -> {
                    showLoading(false)
                }
                is AuthUiState.Loading -> {
                    showLoading(true)
                    clearErrors()
                }
                is AuthUiState.Success -> {
                    showLoading(false)
                    // findNavController().navigate(R.id.action_authFragment_to_main_graph) // ЭТУ СТРОКУ УДАЛЯЕМ
                    Log.i(TAG, "AuthUiState.Success получен. Ожидаем навигацию из MainActivity.")
                    // Если это был signUp, то просто ждем подтверждения по email.
                    // Если это был signIn, MainActivity должна среагировать на SessionStatus.Authenticated.
                    // Можно показать сообщение об успехе, если это signUp.
                    if (!isSignInMode && state.successMessage != null) { // Если была регистрация и есть сообщение
                        Toast.makeText(requireContext(), state.successMessage, Toast.LENGTH_LONG).show()
                    } else if (isSignInMode && state.successMessage != null) {
                        // Для signIn успех означает, что MainActivity должна перенаправить.
                        // Сообщение здесь может быть излишним, если переход быстрый.
                        Log.i(TAG, "Успешный вход, MainActivity должна перенаправить.")
                    }
                }
                is AuthUiState.Error -> {
                    showLoading(false)
                    showError(state.errorMessage)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.buttonPrimary.isEnabled = !isLoading
        // Можно также блокировать поля ввода и кнопку переключения режима
        binding.inputEmail.isEnabled = !isLoading
        binding.inputPassword.isEnabled = !isLoading
        binding.buttonSwitchMode.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        // Улучшенная логика показа ошибок (можно и дальше развивать)
        if (message.contains("email", ignoreCase = true) || message.contains("почта", ignoreCase = true)) {
            binding.inputLayoutEmail.error = message
            binding.inputLayoutPassword.error = null // Сбрасываем ошибку с другого поля
        } else if (message.contains("password", ignoreCase = true) || message.contains("пароль", ignoreCase = true)) {
            binding.inputLayoutPassword.error = message
            binding.inputLayoutEmail.error = null // Сбрасываем ошибку с другого поля
        } else {
            // Общая ошибка, не привязанная к конкретному полю
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
        Log.e(TAG, "Ошибка UI: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView")
    }
}
*/
/*package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAuthBinding
import com.example.myapplication.presentation.AuthUiState
import com.example.myapplication.presentation.AuthViewModel

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    // Флаг для отслеживания режима (true = вход, false = регистрация)
    private var isSignInMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeUiState()
        updateUI()
    }

    private fun setupClickListeners() {
        // Основная кнопка (Войти/Зарегистрироваться)
        binding.buttonPrimary.setOnClickListener {
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            // Скрываем клавиатуру
            it.clearFocus()

            // Вызываем нужный метод в зависимости от режима
            if (isSignInMode) {
                viewModel.signIn(email, password)
            } else {
                viewModel.signUp(email, password)
            }
        }

        // Кнопка переключения режима
        binding.buttonSwitchMode.setOnClickListener {
            isSignInMode = !isSignInMode
            updateUI()
            clearErrors()
        }
    }

    private fun updateUI() {
        if (isSignInMode) {
            // Режим входа
            binding.textTitle.text = "Вход в приложение"
            binding.buttonPrimary.text = "Войти"
            binding.textSwitchMode.text = "Нет аккаунта? "
            binding.buttonSwitchMode.text = "Зарегистрироваться"
        } else {
            // Режим регистрации
            binding.textTitle.text = "Регистрация"
            binding.buttonPrimary.text = "Создать аккаунт"
            binding.textSwitchMode.text = "Уже есть аккаунт? "
            binding.buttonSwitchMode.text = "Войти"
        }
    }

    private fun clearErrors() {
        binding.inputLayoutEmail.error = null
        binding.inputLayoutPassword.error = null
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthUiState.Idle -> {
                    showLoading(false)
                }
                is AuthUiState.Loading -> {
                    showLoading(true)
                    clearErrors()
                }
                is AuthUiState.Success -> {
                    showLoading(false)
                    // Переходим на главный экран
                    findNavController().navigate(R.id.action_authFragment_to_main_graph)
                }
                is AuthUiState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.buttonPrimary.isEnabled = !isLoading
        binding.inputEmail.isEnabled = !isLoading
        binding.inputPassword.isEnabled = !isLoading
        binding.buttonSwitchMode.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        when {
            message.contains("email", ignoreCase = true) -> {
                binding.inputLayoutEmail.error = message
            }
            message.contains("пароль", ignoreCase = true) -> {
                binding.inputLayoutPassword.error = message
            }
            else -> {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

 */