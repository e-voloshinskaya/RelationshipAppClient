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
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(navController.graph.startDestinationId, true)
                                    .setLaunchSingleTop(true)
                                    .build()
                                Log.d(TAG, "observeAuthState: Переход на Splash для редиректа на Auth (очистка стека).")
                                navController.navigate(R.id.splashFragment, null, navOptions)
                            } catch (e: Exception) {
                                Log.e(TAG, "observeAuthState: Ошибка при попытке навигации на AuthFragment при NotAuthenticated: ${e.message}", e)
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
                R.id.authFragment, R.id.splashFragment -> false
                else -> true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}

/*

package com.example.myapplication.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Email успешно подтвержден. Добро пожаловать!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(navController.graph.startDestinationId, true)
                                    .setLaunchSingleTop(true)
                                    .build()
                                Log.d(TAG, "observeAuthState: Переход на Splash для редиректа на Auth (очистка стека).")
                                navController.navigate(R.id.splashFragment, null, navOptions)
                            } catch (e: Exception) {
                                Log.e(TAG, "observeAuthState: Ошибка при попытке навигации на AuthFragment при NotAuthenticated: ${e.message}", e)
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
                R.id.authFragment, R.id.splashFragment, R.id.settingsFragment  -> false
                else -> true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}
*/
/*package com.example.myapplication.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.R
import com.example.myapplication.SupabaseInit
import com.example.myapplication.databinding.ActivityMainBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        lifecycleScope.launch {
            intent?.let {
                try {
                    // Исправлено: вызываем handleDeeplinks у клиента, а не у auth
                    SupabaseInit.client.handleDeeplinks(intent) { session ->
                        println("Диплинк обработан! Сессия: $session")
                        // Автоматическая аутентификация после подтверждения
                        lifecycleScope.launch {
                            SupabaseInit.client.auth.refreshCurrentSession()
                            observeAuthState()
                        }
                    }
                } catch (e: Exception) {
                    println("Ошибка обработки диплинка: ${e.message}")
                }
            }
        }
    }

    private fun observeAuthState() {
        SupabaseInit.client.auth.sessionStatus
            .onEach { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        if (navController.currentDestination?.id == R.id.splashFragment) {
                            navController.navigate(R.id.action_splashFragment_to_main_graph)
                        } else if (navController.currentDestination?.id == R.id.authFragment) {
                            navController.navigate(R.id.action_authFragment_to_main_graph)
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        if (navController.currentDestination?.id == R.id.splashFragment) {
                            navController.navigate(R.id.action_splashFragment_to_authFragment)
                        }
                    }
                    else -> {} // Loading, NetworkError...
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun setupBottomNavVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = when (destination.id) {
                R.id.authFragment, R.id.splashFragment -> false
                else -> true
            }
        }
    }
}
*/
/*
package com.example.myapplication.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.R
import com.example.myapplication.SupabaseInit
import com.example.myapplication.databinding.ActivityMainBinding // <-- Важный импорт!
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

// com/example/myapplication/presentation/MainActivity.kt
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Логику скрытия/показа bottomNav можно оставить здесь
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = when (destination.id) {
                R.id.authFragment, R.id.splashFragment -> false
                else -> true
            }
        }
    }
    // ВСЁ! Больше никакой логики здесь нет.
}

*/

/*
package com.example.myapplication.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.R
import com.example.myapplication.SupabaseInit
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {

        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим NavHostFragment и получаем NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Находим BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Связываем NavController с BottomNavigationView
        bottomNavigationView.setupWithNavController(navController)

        // Проверяем авторизацию
        lifecycleScope.launch {
            val currentUser = SupabaseInit.client.auth.currentUserOrNull()
            // Настраиваем стартовый экран в зависимости от авторизации
            if (currentUser == null) {
                // Не авторизован - показываем экран входа
                navController.navigate(R.id.authFragment)
            }
            // Иначе остаёмся на стартовом экране из nav_graph
        }


    }
}

/*
package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class com.example.myapplication.MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        // привязываем bottomNav к NavController
        findViewById<BottomNavigationView>(R.id.bottomNavigation)
            .setupWithNavController(navController)

        // слушаем смену экрана, чтобы скрыть/показать bottom nav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            when (destination.id) {
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.insightsFragment,
                R.id.profileFragment -> bottomNav.isVisible = true
                else -> bottomNav.isVisible = false
            }
        }
    }
}
*/