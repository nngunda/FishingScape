package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.ChipfragmentBinding
import com.example.myapplication.databinding.FishchipBinding
import com.google.android.material.chip.Chip
import com.google.gson.Gson

class ChipFragment:Fragment() {
    private var _binding:ChipfragmentBinding?=null
    private val binding get()= _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding=ChipfragmentBinding.inflate(inflater,container,false)
        val result=arguments?.getString("fish")
        val fishNamesJson=arguments?.getString("edit_fish")?: result
        val fishNames= Gson().fromJson(fishNamesJson,ArrayList<String>()::class.java)
        val addChip=DataBindingUtil.inflate<FishchipBinding>(LayoutInflater.from(this.activity),
            R.layout.fishchip,
            null,
            false).apply {
            gyoChip= GyoChipType(gyoName = "編集")
            (root as Chip).setOnClickListener {
                parentFragmentManager.beginTransaction().replace(R.id.fish_chip_frame,EditChipFragment().apply {
                    arguments=Bundle().apply {
                        putString("edit_chip_names",fishNamesJson)
                    }
                }).commit()
            }
        }
        binding.fishChipGroup.removeAllViews()
        binding.fishChipGroup.addView(addChip.root)
        for(x in 0 until fishNames.size){
            binding.fishChipGroup.addView(DataBindingUtil.inflate<FishchipBinding>(LayoutInflater.from(this.activity),
                R.layout.fishchip,
                null,
                false).apply {
                gyoChip=GyoChipType(gyoName = fishNames[x])
            }.root)
        }
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}