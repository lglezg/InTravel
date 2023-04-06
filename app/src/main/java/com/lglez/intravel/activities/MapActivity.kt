package com.lglez.intravel.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.lglez.intravel.R
import com.lglez.intravel.databinding.ActivityMapBinding
import com.lglez.intravel.providers.AuthProvider
import com.lglez.intravel.providers.GeoProvider

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private lateinit var vBind : ActivityMapBinding

    private var googleMap : GoogleMap? = null
    private var easyWayLocation : EasyWayLocation? = null
    private var currentLocation : LatLng? = null
    private var geoProvider = GeoProvider()
    private var authProvider = AuthProvider()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = ActivityMapBinding.inflate(layoutInflater)
        setContentView(vBind.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )

        vBind.btnRequestTrip.setOnClickListener {  }
    }

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permission ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            when{
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)->{
                    easyWayLocation?.startLocation()
                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)->{
                    easyWayLocation?.startLocation()
                }
                else ->{

                }
            }
        }

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        googleMap?.isMyLocationEnabled = true

        try {
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )

            if (success != true)
                Log.d("MAPAS", "Error: No se pudo encontrar el estilo")

        } catch (e : Resources.NotFoundException){
            Log.d("MAPAS", "Error: ${e.toString()}")
        }

    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)

        currentLocation?.let {
            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(it).zoom(17f).build()
            ))

        }

    }

    override fun locationCancelled() {

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates();
    }

    override fun onPause() {
        super.onPause()
        easyWayLocation?.endUpdates();
    }






}