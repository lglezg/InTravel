package com.lglez.intravel.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.lglez.intravel.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity() {

    private lateinit var vBind : ActivityMapBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = ActivityMapBinding.inflate(layoutInflater)
        setContentView(vBind.root)
    }
}