package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.EditTextFragmentBinding
import kotlin.properties.Delegates

@SuppressLint("UseRequireInsteadOfGet")
class EditTextFragment:Fragment() {
    private var _binding:EditTextFragmentBinding?=null
    private val binding get()=_binding!!
    private lateinit var helper:DameData
    private lateinit var pointData: PointData
    private lateinit var editTextFragment:Context
    private var _id by Delegates.notNull<Int>()
    private var same=false
    override fun onAttach(context: Context) {
        super.onAttach(context)
        helper=DameData(context)
        pointData= PointData(context)
        editTextFragment=context
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding=EditTextFragmentBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            helper.readableDatabase.use { db->
                db.rawQuery("select _id from dame",null).use { cursor->
                    if(cursor.moveToFirst()){
                        _id=cursor.getInt(0)
                    }
                }
            }
            val name=it.getString(("edit"))!!
            binding.pointNameEdit.setText(name)
            binding.pointNameEdit.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    pointData.readableDatabase.use { db->
                        db.rawQuery("select _id,point_name from point where point_name=?", arrayOf(binding.pointNameEdit.text.toString())).use { cursor->
                            if (cursor.moveToFirst()){
                                do {
                                    if(_id!=cursor.getInt(0)){
                                        same=true
                                    }
                                }while (cursor.moveToNext())
                            }
                        }
                    }
                    if(same||binding.pointNameEdit.text.toString()==""){
                        if(same){
                            Toast.makeText(editTextFragment,"このポイント名は既に使用されています。", Toast.LENGTH_SHORT).show()
                            same=false
                        }else{
                            Toast.makeText(editTextFragment,"ポイント名を記入してください。", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                            v.windowToken,
                            0
                        )
                        val cv=ContentValues().apply {
                            put("point_name",binding.pointNameEdit.text.toString())
                        }
                        helper.writableDatabase.use { db->
                            db?.update("dame",cv,null,null)
                        }
                        parentFragmentManager.beginTransaction().replace(R.id.text,TextFragment().apply {
                            arguments=Bundle().apply {
                                putString("text",binding.pointNameEdit.text.toString())
                            }
                        }).commit()
                    }
                }
                return@setOnEditorActionListener true
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}