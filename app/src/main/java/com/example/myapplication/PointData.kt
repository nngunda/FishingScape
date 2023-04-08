package com.example.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PointData(context: Context):SQLiteOpenHelper(context,"point.sqlite",null,1){
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE if not exists point("+"_id integer PRIMARY KEY not null,point_info text,point_name text ,fish_names text)")
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let{
            it.execSQL("DROP TABLE IF EXISTS point")
            onCreate(it)
        }
    }
    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
    }
}