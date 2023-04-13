package com.lglez.intravel.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lglez.intravel.models.Booking

class BookingProvider {

   private val db = Firebase.firestore.collection("Bookings")
    private val authProvider = AuthProvider()

    fun create( booking: Booking): Task<Void> {
        return db.document(authProvider.getId()).set(booking)
    }

    fun getBooking(): DocumentReference {
        return db.document(authProvider.getId())
    }

    fun remove(clientId: String): Task<Void> {
        return db.document(clientId).delete()
    }
}