package com.example.mobileapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_screen)
        val btnBack = findViewById<Button>(R.id.signUpBackButton)
        btnBack.setOnClickListener{backButtonClicked()}
        val btnSignUp = findViewById<Button>(R.id.signUpButton)
        btnSignUp.setOnClickListener{signUpButtonClicked()}
    }

    private fun backButtonClicked() {
        startActivity(Intent(this, LoginOrSignUpActivity::class.java))
    }

    private fun signUpButtonClicked() {
        val email = findViewById<EditText>(R.id.emailEntry).text.toString()
        val password = findViewById<EditText>(R.id.enterPassword).text.toString()
        val password2 = findViewById<EditText>(R.id.passwordEntry2).text.toString()

        if (email.split("@")[1] != "swansea.ac.uk") {
            buildAlertBox("Email must be from the domain @swansea.ac.uk")
        } else if (password != password2) {
            buildAlertBox("Both passwords must be equal")
        } else if (password.trim().length < 6 ) {
            buildAlertBox("Password must be a length of 6 or more")
        } else {
            UserInformation.getAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        UserInformation.getDatabase().getReference(UserInformation.getCurrentUserID().toString()).child("userType").setValue("user")
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }
        }
    }

        private fun buildAlertBox(text: String) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Error")
            builder.setMessage(text)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }
}
