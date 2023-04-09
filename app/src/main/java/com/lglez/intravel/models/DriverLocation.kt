package com.lglez.intravel.models

import com.google.android.gms.maps.model.LatLng

data class DriverLocation(
    var id: String? = null,
    var latLng: LatLng? = null
)
