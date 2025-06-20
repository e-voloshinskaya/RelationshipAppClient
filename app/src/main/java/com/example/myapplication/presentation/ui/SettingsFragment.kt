package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.SupabaseInit
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.example.myapplication.presentation.AuthUiState
import com.example.myapplication.presentation.AuthViewModel
import com.example.myapplication.presentation.adapters.SettingsAdapter
import com.example.myapplication.presentation.models.SettingsItem
import com.example.myapplication.presentation.models.SettingsSection
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsAdapter: SettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackButton()
        setupRecyclerView()
        loadSettings()
    }

    // Обработка системной кнопки "Назад"
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            findNavController().popBackStack()
        }
    }

    private fun setupBackButton() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                SupabaseInit.client.auth.signOut()

                Toast.makeText(
                    requireContext(),
                    "Вы успешно вышли из аккаунта",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Ошибка при выходе: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupRecyclerView() {
        settingsAdapter = SettingsAdapter { settingItem ->
            // Обработка нажатия на элемент настроек
            when (settingItem.id) {
                "logout" -> {
                    showLogoutConfirmationDialog()
                }
                "pair" -> {
                    findNavController().navigate(R.id.action_settingsFragment_to_pairingFragment)
                }
                else -> {
                    // Навигация к соответствующему экрану настроек
                }
            }
        }

        binding.recyclerSettings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
        }


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, // связываем с жизненным циклом фрагмента
            callback
        )

    }

    private fun loadSettings() {
        val settingsSections = listOf(
            SettingsSection(
                title = "Аккаунт",
                items = listOf(
                    SettingsItem(id = "profile", title = "Профиль", iconResId = R.drawable.ic_user),
                    SettingsItem(id = "change_password", title = "Изменить пароль", iconResId = R.drawable.ic_user),
                    SettingsItem(id = "pair", title = "Связь с партнером", iconResId = R.drawable.ic_pair)
                )
            ),
            SettingsSection(
                title = "Уведомления и безопасность",
                items = listOf(
                    SettingsItem(id = "notifications", title = "Настройки уведомлений", iconResId = R.drawable.ic_bell),
                    SettingsItem(id = "screen_lock", title = "Пароль на вход", iconResId = R.drawable.ic_lock),
                )
            ),
            SettingsSection(
                title = "Помощь",
                items = listOf(
                    SettingsItem(id = "send_feedback", title = "Обратная связь", iconResId = R.drawable.ic_send),
                    SettingsItem(id = "contact_us", title = "Связаться с поддержкой", iconResId = R.drawable.ic_ask),
                    SettingsItem(id = "crisis_helplines", title = "Линии и центры помощи", iconResId = R.drawable.ic_sos),
                )
            ),
            SettingsSection(
                title = "Документы",
                items = listOf(
                    SettingsItem(id = "terms", title = "Условия использования", iconResId = R.drawable.ic_doc),
                    SettingsItem(id = "privacy", title = "Политика конфиденциальности", iconResId = R.drawable.ic_policy), //R.drawable.ic_privacy),
                    //SettingsItem(id = "legal", title = "Правовая информация", iconResId = R.drawable.ic_low), //R.drawable.ic_legal)
                )
            ),
            SettingsSection(
                title = "Другое",
                items = listOf(
                    SettingsItem(id = "useful", title = "Полезные ссылки", iconResId = R.drawable.ic_stars),
                )
            ),
            SettingsSection(
                title = "",
                items = listOf(
                    SettingsItem(id = "logout", title = "Выйти", iconResId = R.drawable.ic_logout),
                    SettingsItem(id = "delete_acc", title = "Удалить аккаунт и связанные данные", iconResId = R.drawable.ic_delete_acc),
                )
            )

        )

        settingsAdapter.submitSections(settingsSections)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}