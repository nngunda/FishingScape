package com.example.myapplication

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class PermissionDialogFragment :DialogFragment(){
    interface PermissionDialogListener{
        fun onPermission(){}
    }
    private lateinit var permissionDialogListener:PermissionDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionDialogListener=(context as PermissionDialogListener)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog=AlertDialog.Builder(activity).apply {
            setTitle("警告")
            setMessage("アプリを再起動して、すべての許可を認可してください")
            setPositiveButton("OK"){ _,_->
                permissionDialogListener.onPermission()
                dismiss()
            }
        }.create()
        return dialog
    }
}