package com.example.mobileapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.edit
import com.google.firebase.database.values

actual class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)
        val btnLogin = findViewById<Button>(R.id.loginToMainButton)
        btnLogin.setOnClickListener{v -> login(v)}
        val btnBack = findViewById<Button>(R.id.loginBackButton)
        btnBack.setOnClickListener{backButtonClicked()}
    }

    actual fun login(view: Any) {
        val typeView = (view as? View)!!
        val userName = findViewById<EditText>(R.id.logInUserNameEntry).text.toString().trim()
        val password = findViewById<EditText>(R.id.logInPasswordEntry).text.toString().trim()
        if (userName.isNotEmpty() && password.isNotEmpty()) {
            UserInformation.getAuth().signInWithEmailAndPassword(userName, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val sharedPreferences = getSharedPreferences("modules", Context.MODE_PRIVATE)

                    val savedModules = UserInformation.getCurrentUserID()
                        ?.let { UserInformation.getDatabase().reference.child(it).child("modules") }
                    savedModules?.get()?.addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val modules = snapshot.value.toString()
                            sharedPreferences.edit().putString("modules", modules).commit()
                        } else {
                            println("No data found at 'modules'")
                        }
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                } else {
                    displayMessage(typeView, "Incorrect User Details")
                }
            }
        } else {
            displayMessage(typeView, "Please fill out all fields")
        }
    }

    actual fun backButtonClicked() {
        startActivity(Intent(this, LoginOrSignUpActivity::class.java))
    }

    actual fun displayMessage(view: Any, text: String) {
        val sb = Snackbar.make(view as View, text, Snackbar.LENGTH_SHORT)
        sb.show()
    }

}