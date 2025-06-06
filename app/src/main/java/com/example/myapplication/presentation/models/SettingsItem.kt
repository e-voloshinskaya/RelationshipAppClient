package com.example.myapplication.presentation.models

import androidx.annotation.DrawableRes

data class SettingsItem(
    val id: String,
    val title: String,
    @DrawableRes val iconResId: Int? = null
)