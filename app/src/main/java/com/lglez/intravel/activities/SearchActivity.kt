package com.lglez.intravel.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.lglez.intravel.databinding.ActivitySearchBinding
import com.lglez.intravel.models.Booking
import com.lglez.intravel.providers.AuthProvider
import com.lglez.intravel.providers.BookingProvider
import com.lglez.intravel.providers.GeoProvider
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener

class SearchActivity : AppCompatActivity() {
    private var listenerBooking: ListenerRegistration? = null
    private lateinit var vBind: ActivitySearchBinding


    private var extraOriginName: String? = null
    private var extraDestinationName: String? = null
    private var extraOriginLat: Double = 0.0
    private var extraOriginLng: Double = 0.0
    private var extraDestinationLat: Double = 0.0
    private var extraDestinationLng: Double = 0.0
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var extraTime: Double = 0.0
    private var extraDistance: Double = 0.0


    private var geoProvider = GeoProvider()
    private var authProvider = AuthProvider()
    private var bookingProvider = BookingProvider()

    private var radius = 0.2
    private var limitRadius = 20
    private var driverId = ""
    private var isDriverFound = false
    private  var driverLatLng : LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(vBind.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        getExtras()
        getClosestDriver()
        checkDriverResponse()
    }

    private fun getExtras() {
        extraOriginName = intent.getStringExtra("origin")
        extraDestinationName = intent.getStringExtra("destination")
        extraOriginLat = intent.getDoubleExtra("origin_lat", 0.0)
        extraOriginLng = intent.getDoubleExtra("origin_lng", 0.0)
        extraTime = intent.getDoubleExtra("time", 0.0)
        extraDistance = intent.getDoubleExtra("distance", 0.0)
        extraDestinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        extraDestinationLng = intent.getDoubleExtra("destination_lng", 0.0)

        originLatLng = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)
    }

    private  fun  createBooking( driverId : String){
        val booking = Booking(
            clientId =  authProvider.getId(),
            driverId = driverId,
            status = "create",
            destination = extraDestinationName,
            origin = extraOriginName,
            time = extraTime,
            distance = extraDistance,
            originLat = extraOriginLat,
            originLng = extraOriginLng,
            destinationLat = extraDestinationLat,
            destinationLng = extraDestinationLng

        )

        bookingProvider.create(booking). addOnCompleteListener {
            if (it.isSuccessful){
                Toast.makeText(this, "Datos del viaje creados", Toast.LENGTH_SHORT).show()
            } else{
                Toast.makeText(this, "Error al crear los datos", Toast.LENGTH_SHORT).show()
            }
        } . addOnFailureListener {
            Log.d("FIRESTORE", "ERROR: ${it.message} ")
        }
    }

    private fun getClosestDriver() {
        originLatLng?.let {
            geoProvider.getNearbyDriver(it, radius)
                .addGeoQueryEventListener(object : GeoQueryEventListener {
                    override fun onGeoQueryError(exception: Exception) {

                    }

                    override fun onGeoQueryReady() {
                        if (!isDriverFound){
                            radius += 0.2

                            if (radius > limitRadius){
                                vBind.tvSearch.text = "NO SE ENCONTRO NINGUN CONDUCTOR"
                                return
                            } else{
                                getClosestDriver()
                            }
                        }
                    }

                    override fun onKeyEntered(documentID: String, location: GeoPoint) {
                        if (!isDriverFound){
                            isDriverFound = true
                            driverId = documentID
                            driverLatLng = LatLng(location.latitude, location.longitude)
                            vBind.tvSearch.text = "CODUCTOR ENCONTRADO\nESPERANDO RESPUESTA"
                            createBooking(documentID)
                        }
                    }

                    override fun onKeyExited(documentID: String) {

                    }

                    override fun onKeyMoved(documentID: String, location: GeoPoint) {

                    }

                }
                )
        }
    }

    private  fun checkDriverResponse(){
       listenerBooking = bookingProvider.getBooking().addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.d("FIRESTORE", "ERROR: ${error.message}")
            }

            if (snapshot != null && snapshot.exists()){
                val booking = snapshot.toObject(Booking::class.java)
                if (booking?.status == "accept"){
                    listenerBooking?.remove()
                    Toast.makeText(this@SearchActivity, "Viaje aceptado", Toast.LENGTH_SHORT).show()
                    goToMapTrip()

                } else if(booking?.status == "cancel"){
                    removeBooking()
                    listenerBooking?.remove()
                    Toast.makeText(this@SearchActivity, "Viaje rechazado", Toast.LENGTH_SHORT).show()
                    goToMap()
                }
            }
        }
    }

    private fun goToMapTrip(){
        val i = Intent(this, MapTripActivity::class.java)
        startActivity(i)

    }
    private fun goToMap(){
        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)

    }

    override fun onDestroy() {
        super.onDestroy()
        listenerBooking?.remove()
    }

    private fun removeBooking() {
        bookingProvider.remove(authProvider.getId())
    }
}