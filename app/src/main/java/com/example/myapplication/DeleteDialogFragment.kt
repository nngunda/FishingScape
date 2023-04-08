package com.example.myapplication

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class DeleteDialogFragment :DialogFragment(){
    interface PointDeleteListener{
        fun onPointDelete(){}
    }
    private lateinit var pointDelete:PointDeleteListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        pointDelete=(context as PointDeleteListener)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog=AlertDialog.Builder(activity).apply {
            setTitle("警告")
            setMessage("本当に削除しますか？")
            setPositiveButton("はい"){ _,_->
                pointDelete.onPointDelete()
                dismiss()
            }
            setNeutralButton("いいえ"){_,_->
                dismiss()
            }
        }.create()
        return dialog
    }
}