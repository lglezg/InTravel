package com.lglez.intravel.models

import com.beust.klaxon.Klaxon

private val klaxon = Klaxon()
data class Booking(
    val id :String? = null,
    val clientId :String? = null,
    val driverId :String? = null,
    val origin :String? = null,
    val destination :String? = null,
    val time :Double? = null,
    val distance :Double? = null,
    val status :String? = null,
    val originLat :Double? = null,
    val originLng :Double? = null,
    val destinationLat :Double? = null,
    val destinationLng :Double? = null,
    val price :Double? = null,
){
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<Booking>(json)
    }
}
