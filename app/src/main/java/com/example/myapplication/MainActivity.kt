package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

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
}