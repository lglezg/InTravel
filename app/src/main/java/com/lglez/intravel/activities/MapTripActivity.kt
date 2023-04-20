package com.lglez.intravel.activities


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.firebase.firestore.ListenerRegistration
import com.lglez.intravel.R
import com.lglez.intravel.databinding.ActivityMapTripBinding
import com.lglez.intravel.models.Booking
import com.lglez.intravel.providers.AuthProvider
import com.lglez.intravel.providers.BookingProvider
import com.lglez.intravel.providers.GeoProvider
import com.lglez.intravel.utils.CarMoveAnim
import kotlin.math.ln

class MapTripActivity : AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack {

    private var endLatLng: LatLng? = null
    private var driverLocation: LatLng? = null
    private var listenerBooking: ListenerRegistration? = null
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var booking: Booking? = null
    private var markerDestination: Marker? = null
    private var markerOrigin: Marker? = null
    private var bookingListener: ListenerRegistration? = null
    private lateinit var vBind: ActivityMapTripBinding

    private var googleMap: GoogleMap? = null
    var easyWayLocation: EasyWayLocation? = null
    private var currentLocation: LatLng? = null
    private var markerDriver: Marker? = null
    private var geoProvider = GeoProvider()
    private var authProvider = AuthProvider()
    private var bookingProvider = BookingProvider()

    private  var wayPoints : ArrayList<LatLng> = ArrayList()
    private  val WAY_POINT_TAG = "way_point_tag"
    private  lateinit var directionUtil: DirectionUtil

    private var isDriverLocationFound = false
    private var isBookingLoaded = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = ActivityMapTripBinding.inflate(layoutInflater)
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
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )


    }

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when {
                    permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {

                    }
                    permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {

                    }
                    else -> {

                    }
                }
            }

        }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true


        getBooking()


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

        googleMap?.isMyLocationEnabled = false

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

    override fun currentLocation(location: Location) {
    }

    override fun locationCancelled() {

    }


    override fun onPause() {
        super.onPause()
        easyWayLocation?.endUpdates();
    }

    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(70, 150, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, 70, 150)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }





    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        directionUtil.drawPath(WAY_POINT_TAG)
    }

    private fun addOriginMarker(position: LatLng){
        markerOrigin = googleMap?.addMarker(
            MarkerOptions().apply {
                position(position)
                title("Mi posición")
                icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person))
            }
        )
    }

    private fun addDriverMarker(position: LatLng){

        markerDriver?.remove()

        markerDriver = googleMap?.addMarker(
            MarkerOptions().apply {
                position(position)
                title("Tu conductor")
                icon(BitmapDescriptorFactory.fromResource(R.drawable.uber_car))
            }
        )
    }
    private fun addDestinationMarker(position: LatLng){
        markerDestination = googleMap?.addMarker(
            MarkerOptions().apply {
                position(position)
                title("Destino")
                icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin))
            }
        )
    }

    private  fun easyDrawRoute(origin: LatLng, destination: LatLng){

        wayPoints.clear()
        wayPoints.add(origin)
        wayPoints.add(destination)
        directionUtil = DirectionUtil.Builder()
            .setDirectionKey("AIzaSyDsTt2fZg_vXx1nIf9IbojSwi5A-35FYnw")
            .setOrigin(origin)
            .setWayPoints(wayPoints)
            .setGoogleMap(googleMap!!)
            .setPolyLinePrimaryColor(R.color.green)
            .setPolyLineWidth(10)
            .setPathAnimation(true)
            .setCallback(this)
            .setDestination(destination)
            .build()
        directionUtil.initPath()


    }

    private  fun getBooking (){
        listenerBooking = bookingProvider.getBooking().addSnapshotListener { document, error ->
            if (error != null) {
                Log.d("FIRESTORE", "ERROR: ${error.message}")
                return@addSnapshotListener
            }



            booking = document?.toObject(Booking::class.java)



            if (!isBookingLoaded ){
                isBookingLoaded = true
                originLatLng = LatLng(booking?.originLat!!, booking!!.originLng!!)
                destinationLatLng = LatLng(booking?.destinationLat!!, booking!!.destinationLng!!)
                getLocationDriver()
                addOriginMarker(originLatLng!!)

                googleMap?.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder().target(originLatLng!!).zoom(17f).build()
                    )
                )
            }


            if (booking?.status == "accept")  vBind.tvStatus.text = "Aceptado"
            if (booking?.status == "started")  {
                vBind.tvStatus.text = "Iniciado"
                startTrip()
            }
            if (booking?.status == "finished") {
                vBind.tvStatus.text = "Finalizado"
                finishTrip()
            }


           // easyDrawRoute(originLatLng!!)



        }
    }

    private fun startTrip() {
        googleMap?.clear()
        driverLocation?.let { addDriverMarker(it) }
        easyDrawRoute(driverLocation!!, destinationLatLng!!)
    }

    private  fun finishTrip(){
        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun getLocationDriver(){
        if (booking != null){
            geoProvider.getLocationWorking(booking?.driverId!!).addSnapshotListener { document, error ->
                if (error != null) {
                    Log.d("FIRESTORE", "ERROR: ${error.message}")
                    return@addSnapshotListener
                }

                if (driverLocation != null){
                    endLatLng = driverLocation
                }

                val l = document?.get("l") as List<*>?
                val lat = l?.get(0) as Double?
                val lng = l?.get(1) as Double?

                if ( lat != null && lng != null){
                    driverLocation = LatLng(lat, lng)
                    driverLocation?.let {
                        //addDriverMarker(it)
                    }
                }


                if (!isDriverLocationFound){

                    isDriverLocationFound = true
                    driverLocation?.let {
                        easyDrawRoute(it, originLatLng!!)
                        addDriverMarker(it)
                    }

                }

                if (endLatLng != null){
                    CarMoveAnim.carAnim(markerDriver!!, endLatLng!!, driverLocation!!)
                }

            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        listenerBooking?.remove()
    }



}