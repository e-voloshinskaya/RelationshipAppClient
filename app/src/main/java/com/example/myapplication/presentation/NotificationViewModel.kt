package com.example.myapplication.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.presentation.models.AppNotification
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import android.util.Log

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository()
    private val TAG = "NotificationViewModel"

    // Состояние: есть ли новые уведомления
    private val _hasNewNotifications = MutableLiveData<Boolean>(false)
    val hasNewNotifications: LiveData<Boolean> = _hasNewNotifications

    // Список уведомлений для экрана истории
    private val _notificationsList = MutableLiveData<List<AppNotification>>()
    val notificationsList: LiveData<List<AppNotification>> = _notificationsList

    // Последнее полученное уведомление для показа pop-up
    private val _latestNotification = MutableLiveData<AppNotification?>()
    val latestNotification: LiveData<AppNotification?> = _latestNotification

    // Состояние загрузки
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        checkUnreadCount()
        listenForNewNotifications()
    }

    // Подписка на новые уведомления через Realtime
    private fun listenForNewNotifications() {
        viewModelScope.launch {
            try {
                repository.subscribeToNotifications(viewModelScope)
                    .onEach { notificationDto ->
                        Log.d(TAG, "New notification received: ${notificationDto.message}")

                        // Устанавливаем флаг новых уведомлений
                        _hasNewNotifications.value = true

                        // Конвертируем и показываем pop-up
                        val appNotification = AppNotification(
                            id = notificationDto.id,
                            message = notificationDto.message,
                            isRead = false,
                            date = "Только что",
                            type = notificationDto.type // Добавляем type
                        )
                        _latestNotification.value = appNotification

                        // Обновляем список, если он был загружен
                        if (_notificationsList.value != null) {
                            loadNotifications()
                        }
                    }
                    .launchIn(viewModelScope)

                Log.d(TAG, "Realtime listener setup successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup realtime listener", e)
            }
        }
    }

    // Проверка количества непрочитанных при запуске
    fun checkUnreadCount() {
        viewModelScope.launch {
            try {
                val unreadCount = repository.getUnreadCount()
                _hasNewNotifications.value = unreadCount > 0
                Log.d(TAG, "Unread notifications count: $unreadCount")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check unread count", e)
            }
        }
    }

    // Загрузка списка всех уведомлений
    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val notifications = repository.getAllNotifications()
                _notificationsList.value = notifications
                Log.d(TAG, "Loaded ${notifications.size} notifications")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load notifications", e)
                _notificationsList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Пометить все уведомления как прочитанные
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                repository.markAllAsRead()
                _hasNewNotifications.value = false

                // Обновляем список, чтобы отразить изменения
                _notificationsList.value = _notificationsList.value?.map {
                    it.copy(isRead = true)
                }

                Log.d(TAG, "All notifications marked as read")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark notifications as read", e)
            }
        }
    }

    // Очистить последнее уведомление (после показа pop-up)
    fun clearLatestNotification() {
        _latestNotification.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Отписываемся от канала при уничтожении ViewModel
        viewModelScope.launch {
            try {
                repository.unsubscribeFromNotifications()
                Log.d(TAG, "Unsubscribed from notifications channel")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from notifications", e)
            }
        }
    }
}