// com/example/myapplication/presentation/ui/SplashFragment.kt
package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val TAG = "SplashFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated. Навигация будет инициирована из MainActivity.")
        // Ничего не делаем здесь. MainActivity.observeAuthState сделает свое дело.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }
}

/*
package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.SupabaseInit
import com.example.myapplication.databinding.FragmentSplashBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Запускаем корутину для проверки авторизации
        lifecycleScope.launch {
            // Можно добавить небольшую задержку для плавности, но это необязательно
            // delay(1000)

            val currentUser = SupabaseInit.client.auth.currentUserOrNull()

            if (isAdded) { // Проверяем, что фрагмент все еще присоединен к Activity
                if (currentUser == null) {
                    // Пользователь не авторизован - на экран входа
                    findNavController().navigate(R.id.action_splashFragment_to_authFragment)
                } else {
                    // Пользователь авторизован - на главный экран
                    findNavController().navigate(R.id.action_splashFragment_to_main_graph)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

 */