package com.lglez.intravel.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.lglez.intravel.databinding.ActivityRegisterBinding
import com.lglez.intravel.models.Client
import com.lglez.intravel.providers.AuthProvider
import com.lglez.intravel.providers.ClientProvider
import com.lglez.intravel.utils.Extensions.isEmail

class RegisterActivity : AppCompatActivity() {
    private lateinit var vBind: ActivityRegisterBinding
    private val authProvider = AuthProvider()
    private val clientProvider = ClientProvider()

    private var name = ""
    private var lastName = ""
    private var email = ""
    private var password = ""
    private var confirmPassword = ""
    private var phone = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(vBind.root)

        setupContent()

    }

    private fun setupContent() {

        vBind.btnRegister.setOnClickListener {
            register()
        }

        vBind.btnLogin.setOnClickListener {
            goToLogin()
        }
    }

    private fun setupForm() {
        name = vBind.edName.text.toString().trim()
        lastName = vBind.edLastName.text.toString().trim()
        email = vBind.edEmail.text.toString().trim()
        password = vBind.edPassword.text.toString().trim()
        confirmPassword = vBind.edConfirmPassword.text.toString().trim()
        phone = vBind.edPhone.text.toString().trim()
    }

    private fun register() {
        if (validate()) {
            authProvider.register(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    val client = Client(
                        id =  authProvider.getId(),
                        name = name,
                        lastName = lastName,
                        phone = phone,
                        email = email
                    )
                    clientProvider.create(client).addOnCompleteListener {
                        if (it.isSuccessful){
                            Toast.makeText(this@RegisterActivity, "Registro exitoso", Toast.LENGTH_SHORT)
                                .show()
                            goToMap()
                        } else{
                            Toast.makeText(
                                this@RegisterActivity,
                                "Registro fallido",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d("DBG", "CLD FIREBASE -> ${it.exception.toString()}")
                        }
                    }

                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registro fallido",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("DBG", "AUTH -> ${it.exception.toString()}")
                }
            }
        }
    }

    private fun validate(): Boolean {
        var isValid = true

        setupForm()

        if (name.isEmpty()) {
            isValid = false
            Toast.makeText(this, "El nombre no puede estar vacio", Toast.LENGTH_SHORT).show()
        }

        if (lastName.isEmpty()) {
            isValid = false
            Toast.makeText(this, "El apellido no puede estar vacio", Toast.LENGTH_SHORT).show()
        }

        if (email.isEmpty()) {
            isValid = false
            Toast.makeText(this, "El correo no puede estar vacio", Toast.LENGTH_SHORT).show()
        } else if (!email.isEmail()) {
            isValid = false
            Toast.makeText(this, "El correo no es valido", Toast.LENGTH_SHORT).show()
        }

        if (password.isEmpty()) {
            isValid = false
            Toast.makeText(this, "La contraseña no puede estar vacia", Toast.LENGTH_SHORT).show()
        } else if (password.length < 6) {
            isValid = false
            Toast.makeText(
                this,
                "La contraseña no puede tener menos 6 caracteres",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (confirmPassword.isEmpty()) {
            isValid = false
            Toast.makeText(
                this,
                "La confirmación de la contraseña no puede estar vacia",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (password != confirmPassword) {
            isValid = false
            Toast.makeText(this, "La contraseña no coincide", Toast.LENGTH_SHORT).show()
        }

        if (phone.isEmpty()) {
            isValid = false
            Toast.makeText(this, "El teléfono no puede estar vacio", Toast.LENGTH_SHORT).show()
        } else if (phone.length < 10) {
            isValid = false
            Toast.makeText(this, "El teléfono debe contener 10 dígitos", Toast.LENGTH_SHORT).show()
        }

        return isValid
    }

    private fun goToLogin() {
        finish()
    }

    private fun goToMap(){
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }
}