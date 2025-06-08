package com.example.myapplication.presentation.models

data class AppNotification(
    val id: String,
    val message: String,
    val isRead: Boolean,
    val date: String,
    val type: String
)