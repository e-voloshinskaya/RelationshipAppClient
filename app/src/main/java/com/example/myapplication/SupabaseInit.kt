package com.example.myapplication

import android.app.Application
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.auth.FlowType

class SupabaseInit : Application() {

    // companion object - это как статические поля в Java.
    // Доступ к ним можно получить через имя класса: SupabaseInit.client
    companion object {
        // lateinit var - мы обещаем, что инициализируем его позже
        lateinit var client: SupabaseClient
            private set // Установить значение можно только внутри этого файла
    }

    override fun onCreate() {
        super.onCreate()

        // Создаем клиент и СРАЗУ ЖЕ присваиваем его нашей статической переменной
        client = createSupabaseClient(
            supabaseUrl = "https://nyoacaexqlvltqyxaalx.supabase.co", // твой URL
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im55b2FjYWV4cWx2bHRxeXhhYWx4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDU3NTY3MzAsImV4cCI6MjA2MTMzMjczMH0.Lzxs5ULg6V7Zd6GL8RrF-ehRVTKlH86w1k4dXgwi3NI"  // твой anon key
        ) {
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
            install(Auth) {
                scheme = "com.example.myapplication" // Как в AndroidManifest.xml
                host = "login-callback" // Как в AndroidManifest.xml
                flowType = FlowType.PKCE // PKCE безопаснее, но можешь оставить IMPLICIT
            }
        }

        Log.d("SupabaseInit", "Supabase Client Initialized!")
    }
}