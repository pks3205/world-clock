package com.forex.sessionclock
import android.app.Application
import com.forex.sessionclock.notifications.NotificationHelper
class ForexClockApp : Application() { override fun onCreate() { super.onCreate(); NotificationHelper.createChannel(this) } }
