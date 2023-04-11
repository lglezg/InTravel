package com.lglez.intravel.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.lglez.intravel.databinding.ActivitySearchBinding
import com.lglez.intravel.providers.AuthProvider
import com.lglez.intravel.providers.GeoProvider
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener

class SearchActivity : AppCompatActivity() {
    private lateinit var vBind: ActivitySearchBinding


    private var extraOriginName: String? = null
    private var extraDestinationName: String? = null
    private var extraOriginLat: Double = 0.0
    private var extraOriginLng: Double = 0.0
    private var extraDestinationLat: Double = 0.0
    private var extraDestinationLng: Double = 0.0
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private var geoProvider = GeoProvider()
    private var authProvider = AuthProvider()

    private var radius = 0.1
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
    }

    private fun getExtras() {
        extraOriginName = intent.getStringExtra("origin")
        extraDestinationName = intent.getStringExtra("destination")
        extraOriginLat = intent.getDoubleExtra("origin_lat", 0.0)
        extraOriginLng = intent.getDoubleExtra("origin_lng", 0.0)
        extraDestinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        extraDestinationLng = intent.getDoubleExtra("destination_lng", 0.0)

        originLatLng = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)
    }

    private fun getClosestDriver() {
        originLatLng?.let {
            geoProvider.getNearbyDriver(it, radius)
                .addGeoQueryEventListener(object : GeoQueryEventListener {
                    override fun onGeoQueryError(exception: Exception) {

                    }

                    override fun onGeoQueryReady() {
                        if (!isDriverFound){
                            radius += 0.1

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
}