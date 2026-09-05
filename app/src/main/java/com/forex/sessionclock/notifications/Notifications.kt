package com.forex.sessionclock.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.forex.sessionclock.MainActivity
import com.forex.sessionclock.R
import com.forex.sessionclock.data.Prefs
import com.forex.sessionclock.domain.*

object NotificationHelper {
 const val CHANNEL="market_events"
 fun createChannel(c:Context) { if(Build.VERSION.SDK_INT>=26) (c.getSystemService(NotificationManager::class.java)).createNotificationChannel(NotificationChannel(CHANNEL,"Forex session events",NotificationManager.IMPORTANCE_DEFAULT).apply{description="Session open, close and overlap alerts"}) }
 fun show(c:Context,title:String,body:String,id:Int) {
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  val open=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  c.getSystemService(NotificationManager::class.java).notify(id,NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setContentIntent(open).setAutoCancel(true).build())
 }
}

object EventScheduler {
 fun schedule(c:Context) {
  val am=c.getSystemService(AlarmManager::class.java)
  SessionEngine.upcomingEvents().take(50).forEach { e ->
   if(!Prefs.enabled(c,e.type.name)) return@forEach
   val intent=Intent(c,EventReceiver::class.java).putExtra("title",e.title).putExtra("body",e.body).putExtra("id",e.id.hashCode()).putExtra("type",e.type.name)
   val pi=PendingIntent.getBroadcast(c,e.id.hashCode(),intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
   if(Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,e.instant.toEpochMilli(),pi)
   else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,e.instant.toEpochMilli(),pi)
  }
 }
}
class EventReceiver:BroadcastReceiver(){ override fun onReceive(c:Context,i:Intent){ if(Prefs.enabled(c,i.getStringExtra("type")?:"")) NotificationHelper.show(c,i.getStringExtra("title")?:"Forex Clock",i.getStringExtra("body")?:"Market update",i.getIntExtra("id",1)); EventScheduler.schedule(c)} }
class RescheduleReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){EventScheduler.schedule(c)} }
