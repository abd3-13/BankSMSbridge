package com.abdelah.banksms

import android.app.Application
import com.abdelah.banksms.logging.RuntimeLogStore

class BankSmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RuntimeLogStore.initialize(this)
    }
}
