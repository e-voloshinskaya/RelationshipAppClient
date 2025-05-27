package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.R


class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
}

/*
package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.Achievement
import com.example.myapplication.R


class com.example.myapplication.ui.ProfileFragment : Fragment(R.layout.fragment_achievements) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Инфлейтим разметку фрагмента
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Находим элементы по ID
        val titleTextView = view.findViewById<TextView>(R.id.homeTitle)
        val clickButton = view.findViewById<Button>(R.id.homeButton)

        // Устанавливаем текст
        titleTextView.text = "Добро пожаловать на главную!"

        // Устанавливаем обработчик клика
        clickButton.setOnClickListener {
            titleTextView.text = "Кнопка была нажата! 👋"
        }

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Подготовь список достижений (например, после загрузки с сервера)
        val achievementsList = listOf(
            Achievement(R.drawable.ic_awful, ContextCompat.getColor(requireContext(), R.color.purple_mild), "Плохое"),
            Achievement(R.drawable.ic_sad, ContextCompat.getColor(requireContext(), R.color.red_mild), "Грустное"),
            Achievement(R.drawable.ic_ok, ContextCompat.getColor(requireContext(), R.color.yellow_mild), "Нормальное"),
            Achievement(R.drawable.ic_happy, ContextCompat.getColor(requireContext(), R.color.green_mild), "Хорошее"),
            Achievement(R.drawable.ic_great, ContextCompat.getColor(requireContext(), R.color.blue_mild), "Отличное")
        )

        // 2. Найди RecyclerView
        val recycler = view.findViewById<RecyclerView>(R.id.achievements_recycler)

        // 3. Установи LayoutManager и Adapter
        recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recycler.adapter = AchievementsAdapter(achievementsList) { achievement ->
            // обработка клика по достижению
            Toast.makeText(requireContext(), "Вы выбрали: ${achievement.name}", Toast.LENGTH_SHORT).show()
        }
    }
}*/
