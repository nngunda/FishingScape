package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.myapplication.databinding.PreviewpointBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import java.io.ByteArrayOutputStream

class PreviewPoint:AppCompatActivity(), OnMapReadyCallback ,DeleteDialogFragment.PointDeleteListener{
    private lateinit var binding:PreviewpointBinding
    private lateinit var point:DataRead
    private val pointData=PointData(this)
    private val dameData=DameData(this)
    private val bitmapData=BitmapData(this)
    private var imagesBinary= mutableListOf<ByteArray>()
    private var imagesBitmap= mutableListOf<Bitmap>()
    private lateinit var deleteView:View
    private val preview=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(RESULT_OK==it.resultCode){
            if(it.data?.getBooleanExtra("DELETE",true)!!){
                imagesBitmap-=(((deleteView as ImageView).drawable) as BitmapDrawable).bitmap
                binding.previewGrid.removeView(deleteView)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= DataBindingUtil.setContentView(this, R.layout.previewpoint)
        val location=Gson().fromJson(intent.getStringExtra("location_edit"),LatLng::class.java)
        pointData.readableDatabase.use{ db->
            db.rawQuery("select * from point",null).use { cursor->
                if (cursor.moveToFirst()){
                    do{
                        if(Gson().fromJson(cursor.getString(1),MarkerOptions::class.java).position==location) {
                            point = DataRead(
                                cursor.getInt(0),
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getString(3),
                            )
                            break
                        }
                    }while (cursor.moveToNext())
                }
            }
        }
        val table="'"+point.pointId.toString()+"'"
        bitmapData.readableDatabase.use { db->
            db.rawQuery("select binary from $table",null).use { cursor->
                if(cursor.moveToFirst()){
                    do {
                        imagesBitmap+=BitmapFactory.decodeByteArray(cursor.getBlob(0),0,cursor.getBlob(0).size)
                    }while (cursor.moveToNext())
                }
            }
        }
        dameData.writableDatabase.use { dbd->
            dbd.delete("dame",null,null)
            val cv=ContentValues().apply {
                put("_id",point.pointId)
                put("point_info",point.pointInfo)
                put("point_name",point.pointName)
                put("fish_names",point.fishNames)
            }
            dbd.insertOrThrow("dame",null,cv)
        }
        supportFragmentManager.beginTransaction().replace(R.id.text,TextFragment().apply {
            arguments=Bundle().apply {
                putString("title", point.pointName)
            }
        }).commit()
        supportFragmentManager.beginTransaction().replace(R.id.fish_chip_frame,ChipFragment().apply{
            arguments=Bundle().apply {
                putString("fish", point.fishNames)
            }
        }).commit()
        for(z in imagesBitmap.indices){
            setImage(imagesBitmap[z])
        }
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            for(x in uris.indices){
                var bitmap=
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val source = ImageDecoder.createSource(contentResolver, uris[x])
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, uris[x])
                    }
                val size=810
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
                setImage(bitmap)
            }
        }.let { lau->
            binding.editAddPic.setOnClickListener {
                lau.launch(arrayOf("image/*"))
            }
        }
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.preview_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        binding.delete.setOnClickListener {
            val dialog=DeleteDialogFragment()
            dialog.show(supportFragmentManager,"delete_dialog")
        }
        binding.edit.setOnClickListener {
            dameData.readableDatabase.use { db->
                db.rawQuery("select * from dame",null).use {
                    if(it.moveToFirst()){
                        val gyoNames=Gson().fromJson(it.getString(3),ArrayList<String>()::class.java)
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
                        val info=MarkerOptions().title(it.getString(2)).snippet("魚種:$fishNames").position(Gson().fromJson(it.getString(1),MarkerOptions()::class.java).position)
                        for(x in imagesBitmap.indices){
                            val byteArrayOutputStream = ByteArrayOutputStream()
                            imagesBitmap[x].compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                            imagesBitmap[x].recycle()
                            byteArrayOutputStream.toByteArray().let { b->
                                println(b.size)
                                imagesBinary+=b
                            }
                            //imagesBinary+=byteArrayOutputStream.toByteArray()
                        }
                        bitmapData.writableDatabase.use { db->
                            var x=0
                            db.rawQuery("select _id from$table",null).use { cursor->
                                if(cursor.moveToFirst()){
                                    do {
                                        if(x<imagesBinary.size){
                                            val cv=ContentValues().apply {
                                                put("binary",imagesBinary[x])
                                            }
                                            db.update(table,cv,"_id=?",
                                                arrayOf(cursor.getInt(0).toString()))
                                            x+=1
                                        }else{
                                            db.delete(table,"_id=?",
                                                arrayOf(cursor.getInt(0).toString()))
                                        }
                                    }while (cursor.moveToNext())
                                }
                            }
                            while (x<imagesBinary.size){
                                val cv=ContentValues().apply {
                                    put("binary",imagesBinary[x])
                                }
                                db.insertOrThrow(table,null,cv)
                                x+=1
                            }
                        }
                        point= DataRead(it.getInt(0),Gson().toJson(info),it.getString(2),it.getString(3))
                    }
                }
            }
            pointData.writableDatabase.use { db->
                val cv=ContentValues().apply {
                    put("_id",point.pointId)
                    put("point_info",point.pointInfo)
                    put("point_name",point.pointName)
                    put("fish_names",point.fishNames)
                }
                db.update("point",cv,"_id=?", arrayOf(point.pointId.toString()))
            }
            setResult(RESULT_OK,intent.putExtra("_id", point.pointId))
            deleteDame()
            finish()
        }
        binding.previewBack.setOnClickListener {
            setResult(RESULT_OK,intent.putExtra("_id", point.pointId))
            deleteDame()
            finish()
        }
    }
    override fun onBackPressed() {
        setResult(RESULT_OK,intent.putExtra("_id", point.pointId))
        deleteDame()
        finish()
    }
    private fun deleteDame(){
        dameData.readableDatabase.use { db->
            db.delete("dame",null,null)
        }
    }
    private fun setImage(bitmap: Bitmap){
        ImageButton(this).apply {
            setImageBitmap(bitmap)
            (100*context.resources.displayMetrics.density).toInt().apply {
                layoutParams= FrameLayout.LayoutParams(this,this)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.WHITE)
            setOnClickListener { im->
                preview.launch(Intent(this@PreviewPoint,Preview::class.java).apply{
                    putExtra("BITMAP",Gson().toJson((((im as ImageView).drawable) as BitmapDrawable).bitmap))
                    deleteView=im
                })
            }
        }.let {
            binding.previewGrid.addView(it)
            it.drawable.callback=null
        }
    }

    override fun onPointDelete() {
        super.onPointDelete()
        pointData.writableDatabase.use { db->
            db.delete("point","_id=?", arrayOf(point.pointId.toString()))
        }
        deleteDame()
        finish()
    }
    override fun onMapReady(map: GoogleMap) {
        val latLng = Gson().fromJson(point.pointInfo, MarkerOptions::class.java).position
        var marker=map.addMarker(MarkerOptions().position(latLng))
        var onMarker: Marker? =null
        val pref=getSharedPreferences("data", Context.MODE_PRIVATE)
        map.mapType = pref.getInt("map_type",1)
        map.uiSettings.isMapToolbarEnabled=false
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        map.setOnMapLongClickListener {
            onMarker?.remove()
            map.addMarker(MarkerOptions().title("マーカーを移動").position(it))?.apply {
                showInfoWindow()
            }.let { m->
                onMarker=m
            }
        }
        map.setOnInfoWindowClickListener {
            var same=true
            pointData.readableDatabase.use { db->
                db.rawQuery("select _id,point_info from point where not _id=?", arrayOf(point.pointId.toString())).use { cursor->
                    if(cursor.moveToFirst()){
                        do {
                            val location=Gson().fromJson(cursor.getString(1),MarkerOptions::class.java).position
                            if(location==it.position){
                                Toast.makeText(this,"この座標は既に使用されています。", Toast.LENGTH_SHORT).show()
                                same=false
                                onMarker?.remove()
                                onMarker=null
                                break
                            }
                        }while (cursor.moveToNext())
                    }
                }
            }
            if(same){
                marker?.remove()
                onMarker?.remove()
                onMarker=null
                dameData.writableDatabase.use { db ->
                    marker=map.addMarker(MarkerOptions().position(it.position))
                    dameData.readableDatabase.use { dbd ->
                        dbd.rawQuery("select point_info from dame", null).use { cursor ->
                            if (cursor.moveToFirst()) {
                                val info =
                                    Gson().fromJson(
                                        cursor.getString(0),
                                        MarkerOptions()::class.java
                                    )
                                info.position(it.position)
                                val cv = ContentValues().apply {
                                    put("point_info", Gson().toJson(info))
                                }
                                db.update("dame", cv, null, null)
                            }
                        }
                    }
                }
            }
        }
        map.setOnMapClickListener {
            onMarker?.remove()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        System.gc()
    }
}