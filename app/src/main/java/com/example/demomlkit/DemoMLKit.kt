package com.example.demomlkit

import android.app.Application

class DemoMLKit : Application() {
    companion object {
        lateinit var demoMLKit: DemoMLKit
    }

    override fun onCreate() {
        super.onCreate()
        demoMLKit = this
    }
}