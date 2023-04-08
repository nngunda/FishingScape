package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.TextFragmentBinding
@SuppressLint("UseRequireInsteadOfGet")
class TextFragment:Fragment() {
    private var _binding:TextFragmentBinding?=null
    private val binding get()=_binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding=TextFragmentBinding.inflate(inflater,container,false)
        val result=arguments?.getString("title")
        arguments?.let { bundle ->
            binding.previewText.text = bundle.getString("text")?:result
        }
        binding.toEditButton.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.text,EditTextFragment().apply {
                arguments=Bundle().apply {
                        putString("edit",binding.previewText.text.toString())
                }
            }).commit()
        }
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}