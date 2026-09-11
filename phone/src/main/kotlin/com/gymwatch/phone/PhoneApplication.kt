package com.gymwatch.phone

import android.app.Application

class PhoneApplication : Application() {
    lateinit var container: PhoneContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = PhoneContainer(this)
    }
}
