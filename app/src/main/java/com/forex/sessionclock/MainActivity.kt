package com.forex.sessionclock

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forex.sessionclock.data.Prefs
import com.forex.sessionclock.domain.EventType
import com.forex.sessionclock.domain.SessionEngine
import com.forex.sessionclock.domain.SessionState
import com.forex.sessionclock.domain.clock
import com.forex.sessionclock.notifications.EventScheduler
import com.forex.sessionclock.widget.ForexWidgetProvider
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        EventScheduler.schedule(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        EventScheduler.schedule(this)
        setContent {
            ForexTheme {
                Dashboard(::pinWidget, ::openAlarmSettings)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun pinWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(
                ComponentName(this, ForexWidgetProvider::class.java),
                null,
                null,
            )
        }
    }

    private fun openAlarmSettings() {
        if (Build.VERSION.SDK_INT >= 31) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(
                    android.net.Uri.parse("package:$packageName"),
                ),
            )
        }
    }
}

@Composable
fun ForexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF07111F),
            surface = Color(0xFF101B2D),
            primary = Color(0xFF32D583),
            onBackground = Color(0xFFF2F4F7),
        ),
        content = content,
    )
}

@Composable
fun Dashboard(
    pin: () -> Unit,
    alarms: () -> Unit,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1_000)
        }
    }

    val states = remember(now) { SessionEngine.states(now) }
    val openSessions = states.filter { it.isOpen }
    val sessionHeadline = if (openSessions.isEmpty()) {
        val nextState = states.minByOrNull { it.remaining }
        "Next session in ${nextState?.remaining?.clock() ?: "--:--:--"}"
    } else {
        openSessions.joinToString(" + ") { it.session.name }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "FOREX",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF32D583),
                        letterSpacing = 3.sp,
                    )
                    Text(
                        text = "Session Clock",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = DateTimeFormatter
                            .ofPattern("EEEE, d MMM · HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(now),
                        color = Color(0xFF98A2B3),
                        fontSize = 13.sp,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = Color(0xFF98A2B3),
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF12263B),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (openSessions.isEmpty()) {
                            "MARKET QUIET"
                        } else {
                            "${openSessions.size} SESSION${if (openSessions.size > 1) "S" else ""} LIVE"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (openSessions.isEmpty()) {
                            Color(0xFF98A2B3)
                        } else {
                            Color(0xFF32D583)
                        },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = sessionHeadline,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            states.forEach { SessionCard(it) }
            Timeline(states)
            NotificationSettings(alarms)

            Button(
                onClick = pin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Add home screen widget")
            }
            Text(
                text = "Session hours are indicative and automatically adjust for daylight saving time. Not financial advice.",
                fontSize = 11.sp,
                color = Color(0xFF667085),
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
fun SessionCard(session: SessionState) {
    val sessionColor = Color(session.session.color)
    val progress = if (session.isOpen) {
        (session.elapsed?.seconds?.div(32_400f) ?: 0f).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF101B2D),
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                if (session.isOpen) Color(0xFF32D583) else Color(0xFF475467),
                                CircleShape,
                            ),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = session.session.name,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            text = session.session.city,
                            fontSize = 12.sp,
                            color = Color(0xFF98A2B3),
                        )
                    }
                }
                Text(
                    text = if (session.isOpen) "OPEN" else "CLOSED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (session.isOpen) Color(0xFF32D583) else Color(0xFF667085),
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = session.remaining.clock(),
                fontSize = 29.sp,
                fontWeight = FontWeight.Light,
                color = sessionColor,
            )
            Text(
                text = if (session.isOpen) {
                    "until close · started ${session.elapsed?.clock() ?: "00:00:00"} ago"
                } else {
                    "until next open"
                },
                fontSize = 12.sp,
                color = Color(0xFF98A2B3),
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = sessionColor,
                trackColor = Color(0xFF24334A),
            )
        }
    }
}

@Composable
fun Timeline(states: List<SessionState>) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF101B2D),
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                text = "TODAY'S WINDOW",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF98A2B3),
            )
            Spacer(modifier = Modifier.height(12.dp))
            states.forEach { session ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = session.session.name,
                        modifier = Modifier.width(82.dp),
                        fontSize = 11.sp,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(Color(0xFF24334A), CircleShape),
                    ) {
                        if (session.isOpen) {
                            val progress = (
                                1 - (session.remaining.seconds / 32_400f)
                            ).coerceIn(0.03f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(Color(session.session.color), CircleShape),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bars show progress while a session is open",
                fontSize = 10.sp,
                color = Color(0xFF667085),
            )
        }
    }
}

@Composable
fun NotificationSettings(openAlarm: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationOptions = listOf(
        EventType.OPEN to "Session opens",
        EventType.CLOSE to "Session closes",
        EventType.OVERLAP to "London + New York overlap",
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF101B2D),
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                text = "NOTIFICATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF98A2B3),
            )
            notificationOptions.forEach { (type, label) ->
                var isEnabled by remember(type) {
                    mutableStateOf(Prefs.enabled(context, type.name))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = label, fontSize = 14.sp)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { enabled ->
                            isEnabled = enabled
                            Prefs.set(context, type.name, enabled)
                            EventScheduler.schedule(context)
                        },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= 31) {
                TextButton(onClick = openAlarm) {
                    Text("Allow precise alert timing")
                }
            }
        }
    }
}
