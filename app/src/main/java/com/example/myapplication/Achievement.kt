package com.example.myapplication

data class Achievement(
    val iconResId: Int,   // R.drawable.ic_...
    val tintColor: Int,   // ContextCompat.getColor(context, R.color....)
    val name: String      // Для contentDescription
)