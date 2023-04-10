package com.lglez.intravel.activities

import android.content.Intent
import android.content.res.Resources
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.lglez.intravel.R
import com.lglez.intravel.databinding.ActivityTripInfoBinding
import com.lglez.intravel.models.Prices
import com.lglez.intravel.providers.ConfigProvider

class TripInfoActivity : AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack {
    private lateinit var vBind: ActivityTripInfoBinding

    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null

    private var extraOriginName: String? = null
    private var extraDestinationName : String? = null
    private var extraOriginLat: Double = 0.0
    private var extraOriginLng : Double = 0.0
    private var extraDestinationLat: Double = 0.0
    private var extraDestinationLng : Double = 0.0

    private  var originLatLng : LatLng ? = null
    private  var destinationLatLng : LatLng ? = null

    private  var wayPoints : ArrayList<LatLng> = ArrayList()
    private  val WAY_POINT_TAG = "way_point_tag"
    private  lateinit var directionUtil: DirectionUtil

    private var markerOrigin : Marker? = null
    private var markerDestination : Marker? = null

    private var configProvider = ConfigProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vBind = ActivityTripInfoBinding.inflate(layoutInflater)
        setContentView(vBind.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        vBind.ivBack.setOnClickListener { finish() }
        vBind.btnConfirmRequest.setOnClickListener { goToSearchDriver() }

        getExtras()
        setUi()
    }

    private fun getPrices(distance: Double, time: Double){
        configProvider.getPrices().addOnSuccessListener { document ->
            if (document.exists()){
                val prices = document.toObject(Prices::class.java)
                val totalDistance = distance * prices?.km!!
                val totalTime = time * prices.min!!
                var total = totalDistance + totalTime

                total = if (total < 5.0) prices?.minValue!! else total

                val minTotal = total - prices?.difference!!
                val maxTotal = total + prices?.difference!!

                val minTotalString = String.format("%.2f", minTotal)
                val maxTotalString = String.format("%.2f", maxTotal)

                vBind.tvPrice.text = "$minTotalString - $maxTotalString"
            }
        }
    }

    private fun setUi() {
       vBind.tvOrigin.text = extraOriginName
       vBind.tvDestination.text = extraDestinationName
    }

    private  fun getExtras(){
        extraOriginName = intent.getStringExtra("origin")
        extraDestinationName = intent.getStringExtra("destination")
        extraOriginLat = intent.getDoubleExtra("origin_lat", 0.0)
        extraOriginLng = intent.getDoubleExtra("origin_lng", 0.0)
        extraDestinationLat = intent.getDoubleExtra("destination_lat",0.0)
        extraDestinationLng = intent.getDoubleExtra("destination_lng", 0.0)

        originLatLng = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)
    }

    private  fun easyDrawRoute(){

        if (originLatLng != null && destinationLatLng != null && googleMap != null){
            wayPoints.add(originLatLng!!)
            wayPoints.add(destinationLatLng!!)
            directionUtil = DirectionUtil.Builder()
                .setDirectionKey("AIzaSyDsTt2fZg_vXx1nIf9IbojSwi5A-35FYnw")
                .setOrigin(originLatLng!!)
                .setWayPoints(wayPoints)
                .setGoogleMap(googleMap!!)
                .setPolyLinePrimaryColor(R.color.green)
                .setPolyLineWidth(10)
                .setPathAnimation(true)
                .setCallback(this)
                .setDestination(destinationLatLng!!)
                .build()
            directionUtil.initPath()
        }

    }

    private fun addOriginMarker(){
        markerOrigin = googleMap?.addMarker(
            MarkerOptions().apply {
                originLatLng?.let { position(it) }
                title("Mi posición")
                icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person))
            }
        )
    }
    private fun addDestinationMarker(){
        markerDestination = googleMap?.addMarker(
            MarkerOptions().apply {
                destinationLatLng?.let { position(it) }
                title("Destino")
                icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin))
            }
        )
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        originLatLng?.let {
            googleMap?.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder().target(it).zoom(12f).build()
                )
            )
        }

        easyDrawRoute()
         addOriginMarker()
        addDestinationMarker()

        try {
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )

            if (success != true)
                Log.d("MAPAS", "Error: No se pudo encontrar el estilo")

        } catch (e: Resources.NotFoundException) {
            Log.d("MAPAS", "Error: ${e.toString()}")
        }
    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location?) {

    }

    override fun locationCancelled() {

    }

    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates();
    }

    override fun onPause() {
        super.onPause()
        easyWayLocation?.endUpdates();
    }

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        var distance = polyLineDetailsArray[1].distance.toDouble() // metros
        var time = polyLineDetailsArray[1].time.toDouble() // segundos
        distance = if (distance < 1000.0) 1000.0 else distance
        time= if (time < 60.0) 60.0 else time
        distance /= 1000
        time /= 60

        val timeString = String.format("%.2f", time)
        val distanceString = String.format("%.2f", distance)

        getPrices(distance, time)

        vBind.tvTimeAndDistance.text = "$timeString mins. - $distanceString km."
        directionUtil.drawPath(WAY_POINT_TAG)
    }

    private fun  goToSearchDriver(){
        val i = Intent( this, SearchActivity::class.java)
        i.putExtra("origin", extraOriginName)
        i.putExtra("destination", extraDestinationName)
        i.putExtra("origin_lat", originLatLng?.latitude)
        i.putExtra("origin_lng", originLatLng?.longitude)
        i.putExtra("destination_lat", destinationLatLng?.latitude)
        i.putExtra("destination_lng", destinationLatLng?.longitude)
        startActivity(i)
    }
}