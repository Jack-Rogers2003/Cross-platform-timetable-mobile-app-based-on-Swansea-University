package com.example.mobileapp


expect class LoginActivity {
    fun login(view: Any)

    fun backButtonClicked()

    fun displayMessage(view: Any, text: String)

    }