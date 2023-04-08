package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.myapplication.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener ,SearchListFragment.ToPointListener,PermissionDialogFragment.PermissionDialogListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var marker: Marker
    private var markP=false
    private var obs=true
    private val helper=PointData(this)
    private val settingResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result->
        if(result.resultCode == RESULT_OK){
            helper.readableDatabase.use { db->
                db.rawQuery("select * from point",null).use {  cursor->
                    if(cursor.moveToFirst()){
                        do{
                            if(cursor.getInt(0)==result.data?.getIntExtra("_id",0)){
                                mMap.addMarker(Gson().fromJson(cursor.getString(1),MarkerOptions::class.java))
                                break
                            }
                        }while (cursor.moveToNext())
                    }
                }
            }
        }
        markP = false
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        binding.search.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(searchWard: String?): Boolean {
                    supportFragmentManager.beginTransaction().replace(R.id.search_list_layout,SearchListFragment().apply {
                        arguments=Bundle().apply {
                            putString("search",searchWard)
                        }
                    }).commit()
                    return false
                }
                override fun onQueryTextChange(searchWard: String?): Boolean {
                    return false
                }
            }
        )
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        requester()
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requester() {
        val permissionList = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        ActivityCompat.requestPermissions(this,permissionList,0)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val pref=getSharedPreferences("data", Context.MODE_PRIVATE)
        mMap.mapType = pref.getInt("map_type",1)
        mMap.uiSettings.isMapToolbarEnabled=false
        binding.search.isSubmitButtonEnabled=true
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED&&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            var providers=locationManager.allProviders
            for(provider in providers){
                locationManager.requestLocationUpdates(provider,500,1f,this)
            }
            helper.readableDatabase.use { db ->
                db.rawQuery("select point_info from point",null).use { cursor->
                    if (cursor.moveToFirst()) {
                        do {
                            mMap.addMarker(Gson().fromJson(cursor.getString(0),MarkerOptions::class.java))
                        }while (cursor.moveToNext())
                    }
                }
            }
            mMap.setOnMapLongClickListener { latLng ->
                obs=false
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                if (markP) {
                    marker.remove()
                }
                marker = mMap.addMarker(MarkerOptions().title("マーカーを追加").position(latLng))!!
                marker.showInfoWindow()
                markP = true
            }
            mMap.setOnMapClickListener {
                if (markP) {
                    marker.remove()
                    markP = false
                }
                binding.searchListLayout.removeAllViews()
            }
            mMap.setOnInfoWindowClickListener {
                if (markP) {
                    marker=it
                    settingResult.launch(Intent(this@MapsActivity, SettingPoint::class.java).apply {
                        putExtra("location_set",Gson().toJson(it.position))
                    })
                    marker.remove()
                } else {
                    marker=it
                    settingResult.launch(Intent(this@MapsActivity, PreviewPoint::class.java).apply {
                        putExtra("location_edit",Gson().toJson(it.position))
                    })
                    marker.remove()
                }
            }
            binding.locationButton.setOnClickListener {
                obs=false
                providers=locationManager.allProviders
                for(provider in providers){
                    locationManager.requestLocationUpdates(provider,500,1f,this)
                }
            }
            binding.mapTypeButton.setOnClickListener {
                var type=pref.getInt("map_type",1)
                val edit=pref.edit()
                if(type==4){
                    edit.putInt("map_type",1)
                    edit.apply()
                    mMap.mapType = 1
                }else{
                    type+=1
                    edit.putInt("map_type",type)
                    edit.apply()
                    mMap.mapType = type
                }
            }
        }else{
            val dialog=PermissionDialogFragment()
            dialog.show(supportFragmentManager,"permission_dialog")
        }
    }
    override fun onLocationChanged(p0: Location) {
        val nowLocation = LatLng(p0.latitude, p0.longitude)
        if(obs){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nowLocation,12f))
        }else{
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nowLocation,15f))
        }
        locationManager.removeUpdates(this)
    }
    override fun onPointed(pointViewJson: String) {
        super.onPointed(pointViewJson)
        val pointView=Gson().fromJson(pointViewJson,MarkerOptions::class.java)
        binding.searchListLayout.removeAllViews()
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pointView.position,15f))
    }
    override fun onPermission() {
        super.onPermission()
        finish()
    }
}