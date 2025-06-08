package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.example.myapplication.databinding.FragmentExploreBinding


class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
        //return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardLearnMain: MaterialCardView = view.findViewById(R.id.card_learn_main)

        cardLearnMain.setOnClickListener {
            // Переход по action из nav_graph.xml
            findNavController().navigate(R.id.action_explore_to_courseModules)
        }

        /* ТЕСТ ТЕСТ ТЕСТ
        binding.buttonTempTestNavigation.setOnClickListener {
            // Здесь мы вызываем action, который только что создали в nav_graph
            findNavController().navigate(R.id.action_exploreFragment_to_pairingFragment)
        }  */

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}