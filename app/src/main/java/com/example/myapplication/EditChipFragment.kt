package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.EditchipfragmentBinding
import com.example.myapplication.databinding.FishchipBinding
import com.example.myapplication.databinding.GyochiplayoutBinding
import com.google.android.material.chip.Chip
import com.google.gson.Gson

class EditChipFragment:Fragment() {
    private var _binding: EditchipfragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var helper:DameData
    override fun onAttach(context: Context) {
        super.onAttach(context)
        helper=DameData(context)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EditchipfragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fishNames = Gson().fromJson(
            arguments?.getString("edit_chip_names"),
            ArrayList<String>()::class.java
        )
        fun addFishChip(fishName:String){
            binding.editChipGroup.addView(DataBindingUtil.inflate<GyochiplayoutBinding>(
                LayoutInflater.from(this.activity),
                R.layout.gyochiplayout,
                null,
                false
            ).apply {
                gyoChip = GyoChipType(gyoName = fishName)
                (root as? Chip)?.setOnCloseIconClickListener {
                    fishNames -= (it as? Chip)?.text.toString()
                    binding.editChipGroup.removeView(root)
                }
            }.root)
        }
        binding.editChipGroup.removeAllViews()
        binding.editChipGroup.addView(
            DataBindingUtil.inflate<FishchipBinding>(
                LayoutInflater.from(this.activity),
                R.layout.fishchip,
                null,
                false
            ).apply {
                gyoChip = GyoChipType(gyoName = "確定")
                (root as Chip).setOnClickListener {
                    val cv = ContentValues().apply {
                        put("fish_names", Gson().toJson(fishNames))
                    }
                    helper.writableDatabase.use { db ->
                        db?.update("dame", cv, null, null)
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fish_chip_frame, ChipFragment().apply {
                            arguments = Bundle().apply {
                                putString("edit_fish", Gson().toJson(fishNames))
                            }
                        }).commit()
                }
            }.root
        )
        for (x in fishNames.indices) {
            addFishChip(fishNames[x])
        }
        binding.fishChipEnter.setOnClickListener {
            if (binding.fishChipName.text.toString() != "") {
                var s = true
                for (x in fishNames.indices) {
                    if (binding.fishChipName.text.toString() == fishNames[x]) {
                        s = false
                        Toast.makeText(context, "その魚種はすでに登録されています。", Toast.LENGTH_SHORT).show()
                        break
                    }
                }
                if (s) {
                    fishNames += binding.fishChipName.text.toString()
                    addFishChip(binding.fishChipName.text.toString())
                    binding.fishChipName.setText("")
                }
            }
        }
        binding.fishChipName.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).
                hideSoftInputFromWindow(v.windowToken,0)
            }
            return@setOnEditorActionListener true
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}