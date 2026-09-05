package com.forex.sessionclock

import android.Manifest
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forex.sessionclock.data.Prefs
import com.forex.sessionclock.domain.*
import com.forex.sessionclock.notifications.EventScheduler
import com.forex.sessionclock.widget.ForexWidgetProvider
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity:ComponentActivity(){
 private val notif=registerForActivityResult(ActivityResultContracts.RequestPermission()){ EventScheduler.schedule(this) }
 override fun onCreate(b:Bundle?){super.onCreate(b); NotificationPermission(); EventScheduler.schedule(this); setContent{ForexTheme{Dashboard(::pinWidget,::openAlarmSettings)}}}
 private fun NotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notif.launch(Manifest.permission.POST_NOTIFICATIONS)}
 private fun pinWidget(){val m=getSystemService(AppWidgetManager::class.java); if(Build.VERSION.SDK_INT>=26&&m.isRequestPinAppWidgetSupported)m.requestPinAppWidget(ComponentName(this,ForexWidgetProvider::class.java),null,null)}
 private fun openAlarmSettings(){if(Build.VERSION.SDK_INT>=31)startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(android.net.Uri.parse("package:$packageName")))}
}

@Composable fun ForexTheme(content:@Composable()->Unit){MaterialTheme(colorScheme=darkColorScheme(background=Color(0xFF07111F),surface=Color(0xFF101B2D),primary=Color(0xFF32D583),onBackground=Color(0xFFF2F4F7)),content=content)}

@Composable fun Dashboard(pin:()->Unit,alarms:()->Unit){
 var now by remember{mutableStateOf(Instant.now())}; LaunchedEffect(Unit){while(true){now=Instant.now();delay(1000)}}
 val states=remember(now){SessionEngine.states(now)}; val events=remember(now.epochSecond/60){SessionEngine.upcomingEvents(now)}
 Scaffold(containerColor=MaterialTheme.colorScheme.background){pad->Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.Top){Column{Text("FOREX",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color(0xFF32D583),letterSpacing=3.sp);Text("Session Clock",fontSize=30.sp,fontWeight=FontWeight.Bold);Text(DateTimeFormatter.ofPattern("EEEE, d MMM · HH:mm:ss").withZone(ZoneId.systemDefault()).format(now),color=Color(0xFF98A2B3),fontSize=13.sp)};Icon(Icons.Outlined.Notifications,null,tint=Color(0xFF98A2B3))}
  val open=states.filter{it.isOpen}; Surface(shape=RoundedCornerShape(20.dp),color=Color(0xFF12263B)){Column(Modifier.padding(18.dp)){Text(if(open.isEmpty())"MARKET QUIET" else "${open.size} SESSION${if(open.size>1)"S" else ""} LIVE",fontSize=12.sp,fontWeight=FontWeight.Bold,color=if(open.isEmpty())Color(0xFF98A2B3) else Color(0xFF32D583));Spacer(Modifier.height(6.dp));Text(if(open.isEmpty())"Next session in ${states.minBy{it.remaining}.remaining.clock()}" else open.joinToString(" + "){it.session.name},fontSize=22.sp,fontWeight=FontWeight.SemiBold)}}
  states.forEach{SessionCard(it)}
  Timeline(states,now)
  NotificationSettings(alarms)
  Button(pin,Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text("Add home screen widget")}
  Text("Session hours are indicative and automatically adjust for daylight saving time. Not financial advice.",fontSize=11.sp,color=Color(0xFF667085),lineHeight=16.sp)
 }}
}

@Composable fun SessionCard(s:SessionState){val c=Color(s.session.color);Surface(shape=RoundedCornerShape(18.dp),color=Color(0xFF101B2D)){Column(Modifier.padding(17.dp)){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(9.dp).background(if(s.isOpen)Color(0xFF32D583) else Color(0xFF475467),CircleShape));Spacer(Modifier.width(10.dp));Column{Text(s.session.name,fontWeight=FontWeight.Bold,letterSpacing=1.sp);Text(s.session.city,fontSize=12.sp,color=Color(0xFF98A2B3))}};Text(if(s.isOpen)"OPEN" else "CLOSED",fontSize=11.sp,fontWeight=FontWeight.Bold,color=if(s.isOpen)Color(0xFF32D583) else Color(0xFF667085))};Spacer(Modifier.height(15.dp));Text(s.remaining.clock(),fontSize=29.sp,fontWeight=FontWeight.Light,color=c);Text(if(s.isOpen)"until close · started ${s.elapsed?.clock()} ago" else "until next open",fontSize=12.sp,color=Color(0xFF98A2B3));Spacer(Modifier.height(12.dp));LinearProgressIndicator(progress={if(s.isOpen)(s.elapsed!!.seconds/32400f).coerceIn(0f,1f) else 0f},Modifier.fillMaxWidth().height(3.dp),color=c,trackColor=Color(0xFF24334A))}}
}

@Composable fun Timeline(states:List<SessionState>,now:Instant){Surface(shape=RoundedCornerShape(18.dp),color=Color(0xFF101B2D)){Column(Modifier.padding(17.dp)){Text("TODAY'S WINDOW",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color(0xFF98A2B3));Spacer(Modifier.height(12.dp));states.forEach{s->Row(Modifier.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Text(s.session.name,Modifier.width(82.dp),fontSize=11.sp);Box(Modifier.weight(1f).height(8.dp).background(Color(0xFF24334A),CircleShape)){if(s.isOpen)Box(Modifier.fillMaxHeight().fillMaxWidth((1-(s.remaining.seconds/32400f)).coerceIn(.03f,1f)).background(Color(s.session.color),CircleShape))}}};Spacer(Modifier.height(6.dp));Text("Bars show progress while a session is open",fontSize=10.sp,color=Color(0xFF667085))}}}

@Composable fun NotificationSettings(openAlarm:()->Unit){val c=androidx.compose.ui.platform.LocalContext.current;Surface(shape=RoundedCornerShape(18.dp),color=Color(0xFF101B2D)){Column(Modifier.padding(17.dp)){Text("NOTIFICATIONS",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color(0xFF98A2B3));listOf(EventType.OPEN to "Session opens",EventType.CLOSE to "Session closes",EventType.OVERLAP to "London + New York overlap").forEach{(type,label)->var on by remember{mutableStateOf(Prefs.enabled(c,type.name))};Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text(label,fontSize=14.sp);Switch(on,{on=it;Prefs.set(c,type.name,it);EventScheduler.schedule(c)})}};if(Build.VERSION.SDK_INT>=31){TextButton(openAlarm){Text("Allow precise alert timing")}}}}}
