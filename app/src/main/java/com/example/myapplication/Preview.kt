package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.davemorrissey.labs.subscaleview.ImageSource
import com.example.myapplication.databinding.PreviewpicBinding
import com.google.gson.Gson

class Preview:AppCompatActivity(){
    private lateinit var binding:PreviewpicBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.previewpic)
        val bitmap= Gson().fromJson(intent.getStringExtra("BITMAP"),Bitmap::class.java)
        binding.preview.setImage(ImageSource.bitmap(bitmap!!))
        binding.delete.setOnClickListener {
            setResult(RESULT_OK,Intent().apply {
                putExtra("DELETE",true)
            })
            finish()
        }
        binding.back.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}