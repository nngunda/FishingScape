package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.CursorWindow
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.myapplication.databinding.ActivitySettingPointBinding
import com.example.myapplication.databinding.GyochiplayoutBinding
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import java.io.ByteArrayOutputStream


class SettingPoint : AppCompatActivity() {
    private lateinit var binding: ActivitySettingPointBinding
    private lateinit var deleteView:View
    private lateinit var location:LatLng
    private lateinit var marker:String
    private var same=false
    private val pointData=PointData(this)
    private val bitmapData=BitmapData(this)
    private var imagesBitmap= mutableListOf<Bitmap>()
    private var imagesBinary= mutableListOf<ByteArray>()
    private var imageViews= mutableListOf<View>()
    private val preview=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(RESULT_OK==it.resultCode){
            if(it.data?.getBooleanExtra("DELETE",true)!!){
                imagesBitmap-=(((deleteView as ImageView).drawable) as BitmapDrawable).bitmap
                binding.imagegridview.removeView(deleteView)
                imageViews-=deleteView
                (deleteView as ImageButton).setImageDrawable(null)
            }
        }
    }
    private val picture=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){ uris ->
        for(x in uris.indices){
            var bitmap=
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val source = ImageDecoder.createSource(contentResolver, uris[x])
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, uris[x])
                }
            val size=1200
            if(bitmap.height>size||bitmap.width>size){
                bitmap = if(bitmap.height>bitmap.width){
                    val width=bitmap.width*size/bitmap.height
                    Bitmap.createScaledBitmap(bitmap,width,size,true)
                }else{
                    val height=bitmap.height*size/bitmap.width
                    Bitmap.createScaledBitmap(bitmap,size,height,true)
                }
            }
            imagesBitmap+=bitmap
            ImageButton(this).apply {
                setImageBitmap(bitmap)
                (100*context.resources.displayMetrics.density).toInt().apply {
                    layoutParams=FrameLayout.LayoutParams(this,this)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.WHITE)
                setOnClickListener {
                    preview.launch(Intent(this@SettingPoint,Preview::class.java).apply{
                        putExtra("BITMAP", Gson().toJson((((it as ImageView).drawable) as BitmapDrawable).bitmap))
                        deleteView=it
                    })
                }
            }.let {
                binding.imagegridview.addView(it)
                imageViews+=it
                it.drawable.callback=null
            }
        }
    }
    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= DataBindingUtil.setContentView(this, R.layout.activity_setting_point)
        val pref: SharedPreferences =getSharedPreferences("data", Context.MODE_PRIVATE)
        val gyoNames= arrayListOf<String>()
        binding.pointName.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).
                hideSoftInputFromWindow(v.windowToken,0)
            }
            return@setOnEditorActionListener true
        }
        binding.set.setOnClickListener{
            pointData.readableDatabase.use { db->
                db.rawQuery("select point_name from point where point_name=?", arrayOf(binding.pointName.text.toString())).use { cursor->
                    if (cursor.moveToFirst()){
                        same=true
                    }
                }
            }
            if(same||binding.pointName.text.toString()==""){
                if(same){
                    Toast.makeText(this,"このポイント名は既に使用されています。", Toast.LENGTH_SHORT).show()
                    same=false
                }else{
                    Toast.makeText(this,"ポイント名を記入してください。", Toast.LENGTH_SHORT).show()
                }
            }else{
                location= Gson().fromJson(intent.getStringExtra("location_set").toString(),LatLng::class.java)
                var fishNames=""
                if (gyoNames.size!=0){
                    if(gyoNames.size<4){
                        for(x in 0 until gyoNames.size-1){
                            fishNames+=gyoNames[x]+","
                        }
                        fishNames+=gyoNames[gyoNames.size-1]
                    }else{
                        for(x in 0 .. 1){
                            fishNames+=gyoNames[x]+","
                        }
                        fishNames+=gyoNames[2]+"..."
                    }
                }
                marker=Gson().toJson(MarkerOptions().title(binding.pointName.text.toString()).snippet("魚種:$fishNames").position(location))
                for(x in imagesBitmap.indices){
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    imagesBitmap[x].compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    imagesBitmap[x].recycle()
                    imagesBinary+=byteArrayOutputStream.toByteArray()
                }
                pointData.writableDatabase.use { db->
                    val cv=ContentValues().apply {
                        var id=pref.getInt("table_id",0)
                        val edit=pref.edit()
                        id+=1
                        println(id)
                        edit.putInt("table_id",id)
                        edit.apply()
                        put("_id",id)
                        put("point_info",marker)
                        put("point_name",binding.pointName.text.toString())
                        put("fish_names",Gson().toJson(gyoNames))
                    }
                    db.insertOrThrow("point",null,cv)
                }
                bitmapData.writableDatabase.use { db->
                    val table="'"+pref.getInt("table_id",1).toString()+"'"
                    db?.execSQL("CREATE TABLE if not exists $table(_id integer PRIMARY KEY,binary blob)")
                    for(x in imagesBinary.indices){
                        val cv=ContentValues().apply {
                            put("binary",imagesBinary[x])
                        }
                        db.insertOrThrow(table,null,cv)
                    }
                }
                pointData.readableDatabase.use{
                    it.query("point", arrayOf("_id","point_info"),null,
                        null,null,null,null,null).use { cursor->
                        if(cursor.moveToFirst()){
                            do {
                                if(cursor.getString(1)==marker){
                                    setResult(RESULT_OK,intent.putExtra("_id",cursor.getInt(0)))
                                    break
                                }
                            }while (cursor.moveToNext())
                        }
                    }
                }
                finish()
            }
        }
        binding.gyosyu.setOnEditorActionListener { v, actionId, _ ->
            if(actionId==EditorInfo.IME_ACTION_DONE){
                if(binding.gyosyu.text.toString()==""){
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).
                    hideSoftInputFromWindow(v.windowToken,0)
                }else{
                    DataBindingUtil.inflate<GyochiplayoutBinding>(LayoutInflater.from(this),
                        R.layout.gyochiplayout,
                        null,
                        false
                    ).apply {
                        gyoChip = GyoChipType(gyoName = binding.gyosyu.text.toString())
                        (root as? Chip)?.setOnCloseIconClickListener {
                            gyoNames-=(it as? Chip)?.text.toString()
                            binding.chipGroup.removeView(root)
                        }
                    }.let {
                        val name = binding.gyosyu.text.toString()
                        var dup = true
                        for (x in gyoNames.indices) {
                            if (name == gyoNames[x]) {
                                dup = false
                                Toast.makeText(this, "その魚種はすでに登録されています。", Toast.LENGTH_SHORT).show()
                                break
                            }
                        }
                        if (dup) {
                            binding.chipGroup.addView(it.root)
                            gyoNames += name
                        }
                    }
                }
                binding.gyosyu.setText("")
            }
            return@setOnEditorActionListener true
        }
        binding.addpicbutton.setOnClickListener {
            picture.launch(arrayOf("image/*"))
        }
        binding.back.setOnClickListener{
            finish()
        }
    }
    override fun onBackPressed(){
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        for(x in imageViews.indices){
            (imageViews[x] as ImageButton).setImageDrawable(null)
        }
        System.gc()
    }
}