package com.lglez.intravel.models

import com.beust.klaxon.*

private val klaxon = Klaxon()

data class Client (
    val id: String? = null,
    val name: String? = null,

    @Json(name = "last_name")
    val lastName: String? = null,

    val email: String? = null,
    val phone: String? = null,
    val image: String? = null
) {
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<Client>(json)
    }
}