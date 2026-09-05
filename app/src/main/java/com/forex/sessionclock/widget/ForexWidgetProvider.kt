package com.forex.sessionclock.widget
import android.app.*
import android.appwidget.*
import android.content.*
import android.widget.RemoteViews
import android.os.SystemClock
import android.os.Build
import com.forex.sessionclock.MainActivity
import com.forex.sessionclock.R
import com.forex.sessionclock.domain.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ForexWidgetProvider:AppWidgetProvider(){
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){ids.forEach{m.updateAppWidget(it,views(c))}}
 override fun onReceive(c:Context,i:Intent){super.onReceive(c,i); if(i.action==Intent.ACTION_TIME_CHANGED||i.action==Intent.ACTION_TIMEZONE_CHANGED) updateAll(c)}
 companion object {
  fun updateAll(c:Context){val m=AppWidgetManager.getInstance(c); val component=ComponentName(c,ForexWidgetProvider::class.java); m.updateAppWidget(component,views(c))}
  private fun views(c:Context):RemoteViews{
   val states=SessionEngine.states(); val v=RemoteViews(c.packageName,R.layout.widget_forex)
   val rows=listOf(
    Triple(R.id.label_asia,R.id.status_asia,R.id.timer_asia),
    Triple(R.id.label_london,R.id.status_london,R.id.timer_london),
    Triple(R.id.label_new_york,R.id.status_new_york,R.id.timer_new_york))
   states.zip(rows).forEach{(s,row)->
    v.setTextViewText(row.first,"${if(s.isOpen) "●" else "○"}  ${s.session.name}")
    v.setTextViewText(row.second,if(s.isOpen) "CLOSES IN" else "OPENS IN")
    v.setTextColor(row.second,c.getColor(if(s.isOpen) R.color.widget_open else R.color.widget_muted))
    val base=SystemClock.elapsedRealtime()+s.remaining.toMillis()
    v.setChronometer(row.third,base,null,true)
    if(Build.VERSION.SDK_INT>=24)v.setChronometerCountDown(row.third,true)
   }
   val next=SessionEngine.upcomingEvents().firstOrNull{it.type==EventType.OVERLAP}
   val fmt=DateTimeFormatter.ofPattern("EEE h:mm a")
   v.setTextViewText(R.id.widget_footer,next?.let{"Next overlap · ${fmt.format(it.instant.atZone(ZoneId.systemDefault()))}"}?:"Tap to open")
   val pi=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT); v.setOnClickPendingIntent(R.id.widget_root,pi)
   return v
  }
 }
}
