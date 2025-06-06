package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.example.myapplication.presentation.adapters.SettingsAdapter
import com.example.myapplication.presentation.models.SettingsItem
import com.example.myapplication.presentation.models.SettingsSection

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


    private fun setupRecyclerView() {
        settingsAdapter = SettingsAdapter { settingItem ->
            // Обработка нажатия на элемент настроек
            when (settingItem.id) {
                "logout" -> {
                    // Реализация выхода из аккаунта
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
                    SettingsItem(id = "profile", title = "Профиль", iconResId = R.drawable.ic_bell), //R.drawable.ic_user),
                    SettingsItem(id = "paired_id", title = "Код для пары", iconResId = R.drawable.ic_bell), //R.drawable.ic_link)
                )
            ),
            SettingsSection(
                title = "Уведомления",
                items = listOf(
                    SettingsItem(id = "notifications", title = "Настройки уведомлений", iconResId = R.drawable.ic_bell)
                )
            ),
            SettingsSection(
                title = "Помощь",
                items = listOf(
                    SettingsItem(id = "help_center", title = "Центр помощи", iconResId = R.drawable.ic_bell), //R.drawable.ic_help),
                    SettingsItem(id = "contact_us", title = "Связаться с нами", iconResId = R.drawable.ic_bell), //R.drawable.ic_mail),
                    SettingsItem(id = "crisis_helplines", title = "Линии помощи", iconResId = R.drawable.ic_bell), //R.drawable.ic_support)
                )
            ),
            SettingsSection(
                title = "Документы",
                items = listOf(
                    SettingsItem(id = "terms", title = "Условия использования", iconResId = R.drawable.ic_bell), //R.drawable.ic_document),
                    SettingsItem(id = "privacy", title = "Политика конфиденциальности", iconResId = R.drawable.ic_bell), //R.drawable.ic_privacy),
                    SettingsItem(id = "legal", title = "Правовая информация", iconResId = R.drawable.ic_bell), //R.drawable.ic_legal)
                )
            ),
            SettingsSection(
                title = "Другое",
                items = listOf(
                    SettingsItem(id = "instagram", title = "Подписаться в Instagram", iconResId = R.drawable.ic_bell),
                    SettingsItem(id = "tiktok", title = "Подписаться в TikTok", iconResId = R.drawable.ic_bell),
                    SettingsItem(id = "logout", title = "Выйти", iconResId = R.drawable.ic_logout)
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