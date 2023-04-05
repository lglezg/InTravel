package com.lglez.intravel.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.lglez.intravel.databinding.ActivityMainBinding
import com.lglez.intravel.providers.AuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var vBind : ActivityMainBinding
    private val authProvider = AuthProvider()

    private var email = ""
    private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vBind.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)


        setupContent()

    }

    override fun onStart() {
        super.onStart()

        if (authProvider.existSession()) goToMap()
    }

    private fun setupContent(){
        vBind.btnLogin.setOnClickListener {
            login()
        }

        vBind.btnRegister.setOnClickListener {
            goToRegister()
        }
    }

    private fun goToRegister() {
       val i = Intent(this, RegisterActivity::class.java)
        startActivity(i)
    }

    private fun setupForm() {
        email = vBind.edEmail.text.toString().trim()
        password = vBind.edPassword.text.toString().trim()
    }

    private fun validate() : Boolean{
        var isValid = true

        if (email.isEmpty()) {
            isValid = false
            Toast.makeText(this, "El correo no puede estar vacio", Toast.LENGTH_SHORT).show()
        }

        if (password.isEmpty()) {
            isValid = false
            Toast.makeText(this, "La contraseña no puede estar vacia", Toast.LENGTH_SHORT).show()
        }

        return isValid
    }

    private fun login(){
        setupForm()
        if (validate()){
            authProvider.login(email, password).addOnCompleteListener {
                if (it.isSuccessful){
                    goToMap()
                } else {
                    Log.d("DBG", "AUTH SINGIN -> ${it.exception.toString()}")
                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun goToMap(){
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }
}