package com.forex.sessionclock.data
import android.content.Context
object Prefs {
 private const val FILE="settings"
 fun enabled(c:Context,type:String)=c.getSharedPreferences(FILE,0).getBoolean(type,true)
 fun set(c:Context,type:String,value:Boolean)=c.getSharedPreferences(FILE,0).edit().putBoolean(type,value).apply()
}
