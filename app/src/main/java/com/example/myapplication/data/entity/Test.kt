package com.example.myapplication.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Test(
    @SerialName("test_id")
    val id: String,
)
