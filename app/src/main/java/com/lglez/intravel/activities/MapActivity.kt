package com.lglez.intravel.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.SphericalUtil
import com.lglez.intravel.R
import com.lglez.intravel.databinding.ActivityMapBinding
import com.lglez.intravel.models.DriverLocation
import com.lglez.intravel.providers.AuthProvider
import com.lglez.intravel.providers.GeoProvider
import com.lglez.intravel.utils.CarMoveAnim
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private lateinit var vBind: ActivityMapBinding

    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null
    private var currentLocation: LatLng? = null
    private var geoProvider = GeoProvider()
    private var authProvider = AuthProvider()

    private var places: PlacesClient? = null
    private var autocompleteOrigin: AutocompleteSupportFragment? = null
    private var autocompleteDestination: AutocompleteSupportFragment? = null
    private var originName = ""
    private var destinationName = ""
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var locationEnable = false
    private val driverMarker = ArrayList<Marker>()
    private val driversLocation = ArrayList<DriverLocation>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = ActivityMapBinding.inflate(layoutInflater)
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

        startGooglePlaces()

        vBind.btnRequestTrip.setOnClickListener { goToTripInfo ()}
    }

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when {
                    permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                        easyWayLocation?.startLocation()
                    }
                    permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                        easyWayLocation?.startLocation()
                    }
                    else -> {

                    }
                }
            }

        }

    private fun startGooglePlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDsTt2fZg_vXx1nIf9IbojSwi5A-35FYnw")
        }

        places = Places.createClient(this)
        initAcOrigin()
        initAcDestination()
    }

    private fun limitSearch() {
        currentLocation?.let {
            val northSide = SphericalUtil.computeOffset(it, 10000.0, 0.0)
            val southSide = SphericalUtil.computeOffset(it, 10000.0, 180.0)

            autocompleteOrigin?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))
            autocompleteDestination?.setLocationBias(
                RectangularBounds.newInstance(
                    southSide,
                    northSide
                )
            )
        }

    }

    private fun initAcOrigin() {
        autocompleteOrigin =
            supportFragmentManager.findFragmentById(R.id.placesAutocompleteOrigin) as AutocompleteSupportFragment
        autocompleteOrigin?.apply {
            setPlaceFields(
                listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS,
                )
            )
            setHint("Lugar de recogida")
            setCountry("MX")
            setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onError(place: Status) {

                }

                override fun onPlaceSelected(place: Place) {
                    originName = place.name ?: ""
                    originLatLng = place.latLng
                }

            })
        }
    }

    private fun initAcDestination() {
        autocompleteDestination =
            supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination) as AutocompleteSupportFragment
        autocompleteDestination?.apply {
            setPlaceFields(
                listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS,
                )
            )
            setHint("Destino")
            setCountry("MX")
            setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onError(place: Status) {

                }

                override fun onPlaceSelected(place: Place) {
                    destinationName = place.name ?: ""
                    destinationLatLng = place.latLng
                }

            })
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        onCameraMove()


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
        currentLocation = LatLng(location.latitude, location.longitude)

        currentLocation?.let {


            if (!locationEnable) {
                locationEnable = true
                googleMap?.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder().target(it).zoom(15f).build()
                    )
                )
                getNearbyDrivers()
                limitSearch()
            }

        }

    }

    override fun locationCancelled() {

    }

    private fun onCameraMove() {
        googleMap?.setOnCameraIdleListener {
            try {
                val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target
                originLatLng?.let {
                    val addressList = geocoder.getFromLocation(
                        originLatLng?.latitude!!,
                        originLatLng?.longitude!!,
                        1
                    )
                    if (addressList.isNotEmpty()) {
                        val city = addressList[0].locality
                        val country = addressList[0].countryName
                        val address = addressList[0].getAddressLine(0)
                        originName = "$address $city"
                        autocompleteOrigin?.setText("$address $city")
                    }

                }

            } catch (e: java.lang.Exception) {
                Log.d("MAPAS", "ERROR: ${e.message}")
            }
        }
    }


    private fun getNearbyDrivers() {
        currentLocation?.let {
            geoProvider.getNearbyDriver(it, 50.0)
                .addGeoQueryEventListener(object : GeoQueryEventListener {
                    override fun onGeoQueryError(exception: Exception) {

                    }

                    override fun onGeoQueryReady() {

                    }

                    override fun onKeyEntered(documentID: String, location: GeoPoint) {
                        for (marker in driverMarker) {
                            if (marker.tag != null) {
                                if (marker.tag == documentID) {
                                    return
                                }
                            }
                        }

                        val driverLatLng = LatLng(location.latitude, location.longitude)
                        val marker = googleMap?.addMarker(
                            MarkerOptions().apply {
                                position(driverLatLng)
                                title("Conductor disponible")
                                icon(
                                    BitmapDescriptorFactory.fromResource(R.drawable.uber_car)
                                )
                            }
                        )

                        marker?.tag = documentID
                        marker?.let { mk -> driverMarker.add(mk) }
                        val dl = DriverLocation()
                        dl.id = documentID
                        driversLocation.add(dl)

                    }

                    override fun onKeyExited(documentID: String) {
                        for (marker in driverMarker) {
                            if (marker.tag != null) {
                                if (marker.tag == documentID) {
                                    marker.remove()
                                    driverMarker.remove(marker)
                                    driversLocation.removeAt(getPositionDriver(documentID))
                                    return
                                }
                            }
                        }
                    }

                    override fun onKeyMoved(documentID: String, location: GeoPoint) {
                        for (marker in driverMarker) {
                            val start = LatLng(location.latitude, location.longitude)
                            var end : LatLng ? = null

                            val position = getPositionDriver(marker.tag.toString())
                            if (marker.tag != null) {
                                if (marker.tag == documentID) {
                                    //marker.position = LatLng(location.latitude, location.longitude)
                                    if (driversLocation[position].latLng != null){
                                        end = driversLocation[position].latLng
                                    }
                                    driversLocation[position].latLng = LatLng(location.latitude, location.longitude)
                                    if (end != null){
                                        CarMoveAnim.carAnim(marker, end, start)
                                    }
                                }
                            }
                        }
                    }

                }

                )
        }
    }

    private  fun getPositionDriver(id: String): Int{
        var position = 0
        for (i in driversLocation.indices){
            if (id == driversLocation[i].id){
                position = i
                break
            }
        }
        return position
    }

    private fun  goToTripInfo(){
        if (originLatLng != null && destinationLatLng != null){
            val i = Intent( this, TripInfoActivity::class.java)
                i.putExtra("origin", originName)
                i.putExtra("destination", destinationName)
                i.putExtra("origin_lat", originLatLng?.latitude)
                i.putExtra("origin_lng", originLatLng?.longitude)
                i.putExtra("destination_lat", destinationLatLng?.latitude)
                i.putExtra("destination_lng", destinationLatLng?.longitude)
            startActivity(i)
        }  else{
            Toast.makeText(this, "Debes seleccionar el origen y el destino", Toast.LENGTH_SHORT).show()
        }


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