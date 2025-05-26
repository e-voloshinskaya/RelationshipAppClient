package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.R

class HistoryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.history_static, container, false)
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
import androidx.fragment.app.Fragment


class com.example.myapplication.ui.HistoryFragment : Fragment() {

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
}*/
