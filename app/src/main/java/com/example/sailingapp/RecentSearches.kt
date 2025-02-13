package com.example.sailingapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RecentSearchesHelper {
    private const val PREF_NAME = "recent_searches"
    private const val KEY_SEARCHES = "search_history"
    private const val MAX_SEARCHES = 5 // Limit to last 5 searches

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSearch(context: Context, query: String) {
        val prefs = getPreferences(context)
        val history = getSearchHistory(context).toMutableList()

        // Remove if already exists & add to the top
        history.remove(query)
        history.add(0, query)

        // Keep only latest N searches
        if (history.size > MAX_SEARCHES) {
            history.removeAt(history.size - 1)
        }

        // Save back to SharedPreferences
        prefs.edit().putString(KEY_SEARCHES, Gson().toJson(history)).apply()
    }

    fun getSearchHistory(context: Context): List<String> {
        val json = getPreferences(context).getString(KEY_SEARCHES, "[]")
        return Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    }
}