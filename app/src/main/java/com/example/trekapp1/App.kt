package com.example.trekapp1

import android.app.Application
import com.example.trekapp1.localDatabase.SyncManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncManager.initialize(this)
    }
}