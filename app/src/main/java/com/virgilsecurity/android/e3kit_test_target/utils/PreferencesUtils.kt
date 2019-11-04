package com.virgilsecurity.android.e3kit_test_target.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * PreferencesUtils
 */
class PreferencesUtils private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun set(name: String, value: String?) {
        sharedPreferences.edit().putString(name, value).apply()
    }

    fun get(name: String): String? = sharedPreferences.getString(name, null)

    fun getNotNull(name: String): String = sharedPreferences.getString(name, null)
        ?: error("$name cannot be null")

    companion object {
        private const val PREFS_NAME = "e3kit_sample_test_prefs"
        const val AUTH_TOKEN = "AUTH_TOKEN"
        const val IDENTITY = "IDENTITY"

        private var instance: PreferencesUtils? = null

        fun instance(context: Context): PreferencesUtils =
            instance ?: synchronized(this) {
                PreferencesUtils(context).also {
                    instance = it
                }
            }
    }
}
