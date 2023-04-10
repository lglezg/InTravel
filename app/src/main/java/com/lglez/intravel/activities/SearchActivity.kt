package com.lglez.intravel.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.lglez.intravel.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {
     private  lateinit var vBind : ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(vBind.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }
}