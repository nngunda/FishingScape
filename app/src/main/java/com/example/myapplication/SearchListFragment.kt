package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.SearchListFragmentBinding
import com.google.gson.Gson

class SearchListFragment:Fragment() {
    private var _binding: SearchListFragmentBinding?=null
    private val binding get()=_binding!!
    private lateinit var helper:PointData
    val data = arrayListOf<Map<String, String>>()
    interface ToPointListener{
        fun onPointed(pointViewJson: String) {}
    }
    private lateinit var toPoint:ToPointListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        helper= PointData(context)
        toPoint=(context as ToPointListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding= SearchListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var searchWard= arguments?.getString("search")
        searchWard=searchWard?.trim()
        var sqlTextPointName=searchWard?.replace("[\\s　]+".toRegex(), "%' OR point_name like'%")
        sqlTextPointName= "point_name like'%$sqlTextPointName%'"
        var sqlTextFishNames=searchWard?.replace("[\\s　]+".toRegex(), "%' OR fish_names like'%")
        sqlTextFishNames= " OR fish_names like'%$sqlTextFishNames%'"
        val sqlText=sqlTextPointName+sqlTextFishNames
        helper.readableDatabase.use { db->
            db.rawQuery("SELECT point_name,fish_names from point WHERE $sqlText",null).use { cursor->
                if(cursor.moveToFirst()){
                    do {
                        val item= HashMap<String,String>()
                        var fishName="魚種:"
                        val namesJson= cursor.getString(1)
                        if(namesJson!="[]"){
                            val name= Gson().fromJson(namesJson,ArrayList<String>()::class.java)
                            for(x in 0..name.size-2){
                                fishName+=name[x]+","
                            }
                            fishName+=name[name.size-1]
                        }
                        item.put("point_name",cursor.getString(0))
                        item.put("fish_names",fishName)
                        data.add(item)
                    }while (cursor.moveToNext())
                }
            }
        }
        binding.searchList.adapter=SimpleAdapter(context,data,android.R.layout.simple_list_item_2,
            arrayOf("point_name","fish_names"), intArrayOf(android.R.id.text1,android.R.id.text2)
        )
        binding.searchList.setOnItemClickListener { _, _, position, _ ->
            helper.readableDatabase.use { db->
                db.rawQuery("select point_info,point_name from point where point_name=?",
                    arrayOf(data[position]["point_name"])
                ).use { cursor->
                    if(cursor.moveToFirst()){
                        toPoint.onPointed(cursor.getString(0))
                    }
                }
            }
        }
    }
}