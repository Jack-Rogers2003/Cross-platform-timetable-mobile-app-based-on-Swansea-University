package com.example.mobileapp;

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button


actual class LoginOrSignUpActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserInformation.getCurrentUser() != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
        setContentView(R.layout.login_or_signup_screen)
        val btnSignUp = findViewById<Button>(R.id.signUpButton)
        btnSignUp.setOnClickListener{signUpButtonClicked()}
        val btnLogin = findViewById<Button>(R.id.loginButton)
        btnLogin.setOnClickListener{loginButtonClicked()}
    }

    actual fun signUpButtonClicked() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    actual fun loginButtonClicked() {
        startActivity(Intent(this, LoginActivity::class.java))
    }
}
