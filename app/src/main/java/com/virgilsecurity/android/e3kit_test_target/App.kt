package com.virgilsecurity.android.e3kit_test_target

import android.app.Application
import com.virgilsecurity.android.e3kit_test_target.utils.PreferencesUtils

/**
 * App
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        PreferencesUtils.instance(this)
    }
}
