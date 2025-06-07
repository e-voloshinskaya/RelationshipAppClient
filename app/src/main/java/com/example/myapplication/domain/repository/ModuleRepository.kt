package com.example.myapplication.domain.repository

import com.example.myapplication.data.entity.ModuleItem

interface ModuleRepository {

    /**
     * Загружает список учебных модулей.
     * @return Result, который содержит либо список модулей при успехе,
     *         либо исключение при ошибке.
     */
    suspend fun getModules(): Result<List<ModuleItem>>
    suspend fun getModulesWithStatus(): List<ModuleItem>
}