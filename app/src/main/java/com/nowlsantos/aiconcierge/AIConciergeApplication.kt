package com.nowlsantos.aiconcierge

import android.app.Application
import android.util.Log
import com.nowlsantos.aiconcierge.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AIConciergeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("ARIS_APP", "Starting Aris...")

        startKoin {
            androidLogger()
            androidContext(this@AIConciergeApplication)
            modules(appModule)
        }
    }
}
