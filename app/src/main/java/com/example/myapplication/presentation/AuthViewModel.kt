package com.example.myapplication.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.SupabaseInit // Убедись, что путь правильный
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.auth.auth // Используем auth из gotrue для Auth плагина
import io.github.jan.supabase.auth.providers.builtin.Email // Провайдер Email
import kotlinx.coroutines.launch

// Состояния экрана авторизации (с полем для сообщения в Success)
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val successMessage: String? = null) : AuthUiState()
    data class Error(val errorMessage: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

// Режим экрана (вход или регистрация)
// Убрал из LiveData, так как это управляется напрямую из Fragment.
// Если ViewModel должна знать об этом для какой-то другой логики, можно вернуть.
// var isSignInMode = true // Управляется из фрагмента

// Убрал toggleMode и authenticate, так как фрагмент теперь сам вызывает signIn/signUp

    fun signIn(email: String, password: String, currentIsSignInMode: Boolean) {
        val (isValid, validationError) = validateInput(email, password)
        if (!isValid) {
            _uiState.value = AuthUiState.Error(validationError ?: "Ошибка валидации")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                SupabaseInit.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                // Успешный вызов signInWith означает, что запрос на сервер прошел.
                // SessionStatus будет обновлен SDK, и MainActivity среагирует.
                // Мы НЕ проверяем currentUserOrNull() здесь для определения успеха входа.
                // Отправляем Success без сообщения, так как навигация будет из MainActivity.
                _uiState.value = AuthUiState.Success()
            } catch (e: RestException) { // Более специфичная обработка ошибок Supabase
                // BadRequestRestException часто содержит полезное сообщение от сервера
                _uiState.value = AuthUiState.Error(e.message ?: "Ошибка входа. Попробуйте снова.")
            } catch (e: Exception) { // Общая обработка других ошибок
                _uiState.value = AuthUiState.Error("Произошла ошибка: ${e.localizedMessage ?: "Попробуйте снова."}")
            }
        }
    }

    fun signUp(email: String, password: String, currentIsSignInMode: Boolean) { // currentIsSignInMode можно убрать, если не используется
        val (isValid, validationError) = validateInput(email, password)
        if (!isValid) {
            _uiState.value = AuthUiState.Error(validationError ?: "Ошибка валидации")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                // ИСПРАВЛЕНО: Используем signUpWith для регистрации
                SupabaseInit.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    // Здесь можно добавить options, если нужно, например, redirectTo:
                    // options { redirectTo = "com.example.myapplication://auth-callback" }
                }
                // Успешный вызов signUpWith. Пользователю нужно подтвердить email.
                // SessionStatus НЕ изменится на Authenticated сразу.
                // Отправляем Success с сообщением для пользователя.
                _uiState.value = AuthUiState.Success("Регистрация почти завершена! Проверьте вашу почту для подтверждения.")
            } catch (e: BadRequestRestException) { // Например, "User already registered"
                _uiState.value = AuthUiState.Error(e.message ?: "Ошибка регистрации. Возможно, такой пользователь уже существует.")
            } catch (e: RestException) {
                _uiState.value = AuthUiState.Error(e.message ?: "Ошибка регистрации. Попробуйте снова.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Произошла ошибка: ${e.localizedMessage ?: "Попробуйте снова."}")
            }
        }
    }

    // validateInput теперь возвращает Pair<Boolean, String?> для результата и сообщения об ошибке
    private fun validateInput(email: String, password: String): Pair<Boolean, String?> {
        return when {
            email.isBlank() -> false to "Введите email"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> false to "Введите корректный email" // Добавил проверку формата email
            password.isBlank() -> false to "Введите пароль"
            password.length < 6 -> false to "Пароль должен быть минимум 6 символов"
            else -> true to null
        }
    }
}
/*package com.example.myapplication.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.SupabaseInit
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email // Провайдер Email
import kotlinx.coroutines.launch

// Состояния экрана авторизации (с полем для сообщения в Success)
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val successMessage: String? = null) : AuthUiState()
    data class Error(val errorMessage: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    // Режим экрана (вход или регистрация)
    // var isSignInMode = true // Управляется из фрагмента

    fun signIn(email: String, password: String, currentIsSignInMode: Boolean) {
        val (isValid, validationError) = validateInput(email, password)
        if (!isValid) {
            _uiState.value = AuthUiState.Error(validationError ?: "Ошибка валидации")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                SupabaseInit.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                // Успешный вызов signInWith означает, что запрос на сервер прошел.
                // SessionStatus будет обновлен SDK, и MainActivity среагирует.
                // Мы НЕ проверяем currentUserOrNull() здесь для определения успеха входа.
                // Отправляем Success без сообщения, так как навигация будет из MainActivity.
                _uiState.value = AuthUiState.Success()
            } catch (e: RestException) { // Более специфичная обработка ошибок Supabase
                // BadRequestRestException часто содержит полезное сообщение от сервера
                _uiState.value = AuthUiState.Error(e.message ?: "Ошибка входа. Попробуйте снова.")
            } catch (e: Exception) { // Общая обработка других ошибок
                _uiState.value = AuthUiState.Error("Произошла ошибка: ${e.localizedMessage ?: "Попробуйте снова."}")
            }
        }
    }

    fun signUp(email: String, password: String, currentIsSignInMode: Boolean) { // currentIsSignInMode можно убрать, если не используется
        val (isValid, validationError) = validateInput(email, password)
        if (!isValid) {
            _uiState.value = AuthUiState.Error(validationError ?: "Ошибка валидации")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                SupabaseInit.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    // Здесь можно добавить options, если нужно, например,
                    //options { redirectTo = "com.example.myapplication://login-callback" }
                }
                // Успешный вызов signUpWith. Пользователю нужно подтвердить email.
                // SessionStatus НЕ изменится на Authenticated сразу.
                // Отправляем Success с сообщением для пользователя.
                _uiState.value = AuthUiState.Success("Регистрация почти завершена! Проверьте вашу почту для подтверждения.")
            } catch (e: BadRequestRestException) { // Например, "User already registered"
                _uiState.value = AuthUiState.Error(e.message ?: "Ошибка регистрации. Возможно, такой пользователь уже существует.")
            } catch (e: RestException) {
                _uiState.value = AuthUiState.Error(e.message ?: "Ошибка регистрации. Попробуйте снова.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Произошла ошибка: ${e.localizedMessage ?: "Попробуйте снова."}")
            }
        }
    }

    // validateInput теперь возвращает Pair<Boolean, String?> для результата и сообщения об ошибке
    private fun validateInput(email: String, password: String): Pair<Boolean, String?> {
        return when {
            email.isBlank() -> false to "Введите email"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> false to "Введите корректный email" // Добавил проверку формата email
            password.isBlank() -> false to "Введите пароль"
            password.length < 6 -> false to "Пароль должен быть минимум 6 символов"
            else -> true to null
        }
    }
}
*/
/*package com.example.myapplication.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.SupabaseInit
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

// Состояния экрана авторизации
sealed class AuthUiState {
    object Idle : AuthUiState() // Начальное состояние
    object Loading : AuthUiState() // Загрузка
    object Success : AuthUiState() // Успешная авторизация
    data class Error(val message: String) : AuthUiState() // Ошибка
}

class AuthViewModel : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    // Режим экрана (вход или регистрация)
    private val _isSignInMode = MutableLiveData(true)
    val isSignInMode: LiveData<Boolean> = _isSignInMode

    fun toggleMode() {
        _isSignInMode.value = !(_isSignInMode.value ?: true)
        _uiState.value = AuthUiState.Idle // Сбрасываем ошибки
    }

    fun authenticate(email: String, password: String) {
        if (_isSignInMode.value == true) {
            signIn(email, password)
        } else {
            signUp(email, password)
        }
    }

    fun signIn(email: String, password: String) {
        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            try {
                // signInWith теперь ничего не возвращает
                SupabaseInit.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                // Проверяем текущего пользователя после входа
                val currentUser = SupabaseInit.client.auth.currentUserOrNull()

                if (currentUser != null) {
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Не удалось войти")
                }

            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Неверный email или пароль")
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            try {
                // signUpWith тоже ничего не возвращает
                SupabaseInit.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // После регистрации проверяем
                val currentUser = SupabaseInit.client.auth.currentUserOrNull()

                if (currentUser != null) {
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Проверьте email для подтверждения")
                }

            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Ошибка регистрации: ${e.message}")
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = AuthUiState.Error("Введите email")
                false
            }
            password.isBlank() -> {
                _uiState.value = AuthUiState.Error("Введите пароль")
                false
            }
            password.length < 6 -> {
                _uiState.value = AuthUiState.Error("Пароль должен быть минимум 6 символов")
                false
            }
            else -> true
        }
    }
}

 */