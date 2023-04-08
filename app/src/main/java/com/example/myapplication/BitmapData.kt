package com.example.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BitmapData(context: Context):SQLiteOpenHelper(context,"bitmap.sqlite",null,1){
    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
    }
}