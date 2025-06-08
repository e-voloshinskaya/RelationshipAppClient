package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.example.myapplication.R
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.example.myapplication.databinding.FragmentExploreBinding
import com.example.myapplication.presentation.NotificationViewModel
import com.example.myapplication.presentation.models.AppNotification


class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    private val notificationViewModel: NotificationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
        //return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardLearnMain: MaterialCardView = view.findViewById(R.id.card_learn_main)

        cardLearnMain.setOnClickListener {
            // Переход по action из nav_graph.xml
            findNavController().navigate(R.id.action_explore_to_courseModules)
        }

        /* ТЕСТ ТЕСТ ТЕСТ
        binding.buttonTempTestNavigation.setOnClickListener {
            // Здесь мы вызываем action, который только что создали в nav_graph
            findNavController().navigate(R.id.action_exploreFragment_to_pairingFragment)
        }  */

        // Добавляем настройку уведомлений
        setupNotifications()

    }

    private fun setupNotifications() {
        // Наблюдаем за состоянием новых уведомлений
        notificationViewModel.hasNewNotifications.observe(viewLifecycleOwner) { hasNew ->
            val iconRes = if (hasNew) {
                R.drawable.ic_bell_active // Активная иконка
            } else {
                R.drawable.ic_bell
            }
            binding.iconNotificationsExplore.setIconResource(iconRes)
        }

        // Наблюдаем за новыми уведомлениями для показа pop-up
        notificationViewModel.latestNotification.observe(viewLifecycleOwner) { notification ->
            notification?.let {
                showNotificationPopup(it)
                notificationViewModel.clearLatestNotification()
            }
        }

        // Клик по иконке уведомлений
        binding.iconNotificationsExplore.setOnClickListener {
            findNavController().navigate(R.id.action_exploreFragment_to_notificationsFragment)
        }
    }

    private fun showNotificationPopup(notification: AppNotification) {
        Snackbar.make(
            binding.root,
            notification.message,
            Snackbar.LENGTH_LONG
        ).setAction("Посмотреть") {
            findNavController().navigate(R.id.action_exploreFragment_to_notificationsFragment)
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}