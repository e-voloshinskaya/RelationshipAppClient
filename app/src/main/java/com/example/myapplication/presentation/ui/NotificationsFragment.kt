package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentNotificationsBinding
import com.example.myapplication.presentation.adapters.NotificationsAdapter
import com.example.myapplication.presentation.NotificationViewModel

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationViewModel by activityViewModels()
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Загружаем уведомления и помечаем как прочитанные
        viewModel.loadNotifications()
        viewModel.markAllNotificationsAsRead()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter()
        binding.recyclerNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationsFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.notificationsList.observe(viewLifecycleOwner) { notifications ->
            adapter.submitList(notifications)
            binding.emptyView.isVisible = notifications.isEmpty()
            binding.recyclerNotifications.isVisible = notifications.isNotEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    private fun setupClickListeners() {
        binding.buttonBackNotif.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}