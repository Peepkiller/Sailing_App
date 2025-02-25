package com.example.sailingapp

import android.content.Context
import android.content.SharedPreferences

object LoginHelper {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"

    fun saveUser(context: Context, username: String, password: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            apply()
        }
    }
    fun isUserLoggedIn(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedOut = prefs.getBoolean("is_logged_out", false)
        val hasCredentials = !prefs.getString(KEY_USERNAME, "").isNullOrEmpty() && !prefs.getString(KEY_PASSWORD, "").isNullOrEmpty()
        return hasCredentials && !isLoggedOut
    }

    fun validateUser(context: Context, username: String, password: String): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isValid = prefs.getString(KEY_USERNAME, "") == username && prefs.getString(KEY_PASSWORD, "") == password
        if (isValid) {
            prefs.edit().apply {
                putBoolean("is_logged_out", false)  // Reset logout flag
                apply()
            }
        }
        return isValid
    }

    fun logout(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply{
            putBoolean("is_logged_out", true)
            apply()
        }
    }
}
