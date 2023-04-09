package com.lglez.intravel.providers

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.GeoQuery

class GeoProvider {
    val collection = FirebaseFirestore.getInstance().collection("Locations")
    val geoFirestore = GeoFirestore(collection)
    fun  saveLocation(driverId: String, position: LatLng){
        geoFirestore.setLocation(driverId, GeoPoint(position.latitude, position.longitude))
    }

    fun removeLocation(driverId: String){
        collection.document(driverId).delete()
    }

    fun getLocation(driverId: String)  : Task<DocumentSnapshot> {
        return collection.document(driverId).get().addOnFailureListener { exception ->
            Log.d("FIREBASE", "ERROR: ${exception.toString()}")
        }
    }

    fun getNearbyDriver( position: LatLng, radius : Double) : GeoQuery{
        val query = geoFirestore.queryAtLocation(GeoPoint(position.latitude, position.longitude), radius)
        query.removeAllListeners()
        return  query
    }
}