package com.example

import android.app.Application
import com.example.data.BridgeManager
import com.example.service.BridgeService

class BridgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BridgeManager.init(this)

        if (BridgeManager.autoStart.value) {
            BridgeService.start(this)
        }
    }
}
