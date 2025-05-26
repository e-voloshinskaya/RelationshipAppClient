package com.example.myapplication.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://nyoacaexqlvltqyxaalx.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im55b2FjYWV4cWx2bHRxeXhhYWx4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDU3NTY3MzAsImV4cCI6MjA2MTMzMjczMH0.Lzxs5ULg6V7Zd6GL8RrF-ehRVTKlH86w1k4dXgwi3NI"
) {
    install(Postgrest)
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим NavHostFragment и получаем NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Находим BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Связываем NavController с BottomNavigationView
        bottomNavigationView.setupWithNavController(navController)
    }
}

/*
package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class com.example.myapplication.MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        // привязываем bottomNav к NavController
        findViewById<BottomNavigationView>(R.id.bottomNavigation)
            .setupWithNavController(navController)

        // слушаем смену экрана, чтобы скрыть/показать bottom nav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            when (destination.id) {
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.insightsFragment,
                R.id.profileFragment -> bottomNav.isVisible = true
                else -> bottomNav.isVisible = false
            }
        }
    }
}*/
