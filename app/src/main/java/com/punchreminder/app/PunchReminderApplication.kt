package com.punchreminder.app

import android.app.Application

class PunchReminderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiContainer.init(this)
    }
}
