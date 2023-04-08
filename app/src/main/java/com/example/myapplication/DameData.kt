package com.example.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DameData(context: Context):SQLiteOpenHelper(context,"dame.sqlite",null,1){
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE if not exists dame("+"_id integer PRIMARY KEY,point_info text,point_name text ,fish_names text)")
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let{
            it.execSQL("DROP TABLE IF EXISTS dame")
            onCreate(it)
        }
    }
    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
    }
}