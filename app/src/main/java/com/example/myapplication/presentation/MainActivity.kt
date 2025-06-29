package com.example.myapplication.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.R
import com.example.myapplication.SupabaseInit
import com.example.myapplication.databinding.ActivityMainBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
// Если RefreshFailureCause нужен для проверки cause в SessionStatus.RefreshFailure
import io.github.jan.supabase.auth.status.RefreshFailureCause // Импорт для RefreshFailureCause
// import io.github.jan.supabase.gotrue.user.UserSession // Убедись, что этот импорт есть, если UserSession используется
import kotlinx.coroutines.flow.launchIn // distinctUntilChanged убран
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        setupBottomNavVisibility()

        handleIntent(intent)
        observeAuthState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: $intent, data: ${intent.dataString}")
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            Log.d(TAG, "handleIntent: обрабатываем deeplink с data: ${intent.dataString}")
            lifecycleScope.launch {
                try {
                    SupabaseInit.client.handleDeeplinks(intent) { userSession ->
                        Log.i(TAG, "Deeplink успешно обработан! Новая сессия: $userSession")
                        // SDK должен сам обновить sessionStatus.
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка обработки deeplink: ${e.message}", e)
                }
            }
        } else {
            Log.d(TAG, "handleIntent: intent не является deeplink или не имеет данных. Action: ${intent?.action}, Data: ${intent?.dataString}")
        }
    }

    private fun observeAuthState() {
        Log.d(TAG, "observeAuthState: ПОДПИСКА НА ИЗМЕНЕНИЯ СТАТУСА СЕССИИ")
        SupabaseInit.client.auth.sessionStatus
            // .distinctUntilChanged() // УБРАНО, так как sessionStatus это StateFlow
            .onEach { status ->
                Log.d(TAG, "observeAuthState: Новый статус = $status. Текущий экран: ${navController.currentDestination?.label} (ID: ${navController.currentDestination?.id})")

                if (navController.currentDestination == null) {
                    Log.w(TAG, "observeAuthState: currentDestination is null. Навигация отложена.")
                    return@onEach
                }
                val currentDestId = navController.currentDestination!!.id

                when (status) {
                    is SessionStatus.Authenticated -> {
                        Log.i(TAG, "observeAuthState: Пользователь АУТЕНТИФИЦИРОВАН.")
                        if (currentDestId == R.id.splashFragment) {
                            Log.d(TAG, "observeAuthState: Навигация Splash -> MainGraph")
                            navController.navigate(R.id.action_splashFragment_to_main_graph)
                        } else if (currentDestId == R.id.authFragment) {
                            Log.d(TAG, "observeAuthState: Навигация Auth -> MainGraph")
                            navController.navigate(R.id.action_authFragment_to_main_graph)
                        } else {
                            Log.d(TAG, "observeAuthState: Аутентифицирован, но не на Splash/Auth. Текущий: $currentDestId. Действий не требуется.")
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        Log.i(TAG, "observeAuthState: Пользователь НЕ АУТЕНТИФИЦИРОВАН.")
                        if (currentDestId == R.id.splashFragment) {
                            Log.d(TAG, "observeAuthState: Навигация Splash -> AuthFragment")
                            navController.navigate(R.id.action_splashFragment_to_authFragment)
                        }
                        else if (currentDestId != R.id.authFragment) {
                            Log.w(TAG, "observeAuthState: NotAuthenticated, но не на Splash/Auth (текущий: $currentDestId). Перенаправление на Auth.")
                            try {
                                // ИЗМЕНЕНИЕ: Переходим НАПРЯМУЮ на authFragment
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_graph, true) // Очищаем весь граф
                                    .build()

                                Log.d(TAG, "observeAuthState: Прямой переход на AuthFragment (очистка стека).")
                                navController.navigate(R.id.authFragment, null, navOptions)
                            } catch (e: Exception) {
                                Log.e(TAG, "observeAuthState: Ошибка при попытке навигации на AuthFragment: ${e.message}", e)
                            }
                        } else {
                            Log.d(TAG, "observeAuthState: NotAuthenticated, уже на AuthFragment. Действий не требуется.")
                        }
                    }
                    SessionStatus.Initializing -> {
                        Log.d(TAG, "observeAuthState: Статус сессии: Initializing...")
                    }
                    is SessionStatus.RefreshFailure -> {
                        // ИСПРАВЛЕНО: status.cause не передается как Throwable
                        Log.e(TAG, "observeAuthState: Статус сессии: RefreshFailure. Причина: ${status.cause}")
                        // Ты можешь проверить конкретную причину, если нужно:
                        when (status.cause) {
                            is RefreshFailureCause.NetworkError -> {
                                Log.e(TAG, "RefreshFailure из-за ошибки сети.")
                                // Показать пользователю сообщение об ошибке сети
                            }
                            is RefreshFailureCause.InternalServerError -> {
                                Log.e(TAG, "RefreshFailure из-за ошибки сервера.")
                                // Показать пользователю сообщение об ошибке сервера
                            }
                            // Добавь is RefreshFailureCause.Unknown, если он есть и его нужно обработать
                        }
                    }
                    else -> {
                        Log.d(TAG, "observeAuthState: Статус сессии: $status (не обработан явно).")
                    }
                }
            }
            .launchIn(lifecycleScope)
    }


    private fun setupBottomNavVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = when (destination.id) {
                R.id.authFragment, R.id.splashFragment, R.id.settingsFragment -> false
                else -> true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}
