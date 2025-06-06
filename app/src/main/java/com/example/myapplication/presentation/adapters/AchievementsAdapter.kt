package com.example.myapplication.presentation.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.presentation.Achievement

class AchievementsAdapter(
    private val items: List<Achievement>,
    private val onClick: (Achievement) -> Unit
) : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageButton = view.findViewById(R.id.achievement_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = items[position]
        holder.icon.setImageResource(achievement.iconResId)
        holder.icon.backgroundTintList = ColorStateList.valueOf(achievement.tintColor)
        holder.icon.contentDescription = achievement.name
        holder.icon.setOnClickListener { onClick(achievement) }
    }

    override fun getItemCount() = items.size
}