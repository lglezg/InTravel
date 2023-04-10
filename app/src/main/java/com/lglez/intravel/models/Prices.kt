package com.lglez.intravel.models
import com.beust.klaxon.*

private val klaxon = Klaxon()
data class Prices(
    val difference: Double? = null,
    val km: Double? = null,
    val min: Double? = null,
    val minValue: Double? = null,
){
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<Prices>(json)
    }
}
