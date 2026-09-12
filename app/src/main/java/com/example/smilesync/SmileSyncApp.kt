package com.example.smilesync

import android.app.Application
import com.example.smilesync.api.RetrofitClient

class SmileSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
