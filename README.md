# Forex Session Clock — Research & Product Plan

यह Android app और home-screen widget Forex के तीन मुख्य trading sessions को local time में दिखाने के लिए प्रस्तावित है:

1. **Asian (Tokyo)**
2. **London**
3. **New York**

> यह trading/advisory app नहीं होगा। Session times indicative हैं; broker और holidays के कारण वास्तविक liquidity/trading availability अलग हो सकती है।

## User को क्या दिखेगा

### Home-screen widget

एक नज़र में:

- अभी कौन-सा session **OPEN** है
- session कितने समय पहले शुरू हुआ (`Started 2h 18m ago`)
- बंद होने में कितना समय बाकी है (`Closes in 4h 42m`)
- बंद session के लिए अगला open (`Opens in 1h 25m`)
- London–New York overlap को अलग highlight
- weekend पर `Market closed · Opens Monday …`
- समय phone के local timezone में

Suggested medium widget:

```text
FOREX SESSIONS                 14:32
● ASIA       OPEN        closes in 00:28
● LONDON     OPEN        closes in 07:28
○ NEW YORK   in 02:28    opens 17:00

Next: London + New York overlap in 02:28
```

### Main app

- 24-hour horizontal timeline
- तीन session cards: open/closed, local open–close, elapsed/remaining
- current-time marker
- overlaps और high-activity window
- timezone selector: Device / IST / UTC / custom IANA timezone
- 12/24-hour format
- session-open, overlap-start और session-close alerts
- widget theme/configuration: compact, medium, detailed; dark/light/system

## Session-time model

Times को fixed UTC offsets के रूप में hard-code नहीं करना चाहिए। London और New York daylight-saving time अलग तारीखों पर बदलते हैं। इसलिए IANA zones और local market hours से हर दिन का UTC interval calculate होगा।

| Session | IANA zone | Indicative local hours |
|---|---|---|
| Asian / Tokyo | `Asia/Tokyo` | 09:00–18:00 JST |
| London | `Europe/London` | 08:00–17:00 London time |
| New York | `America/New_York` | 08:00–17:00 New York time |

इस model से Android की timezone database DST परिवर्तन संभालती है। App settings में times editable रखना बेहतर होगा, क्योंकि अलग sources/brokers session boundary को थोड़ा अलग define कर सकते हैं। Sydney को v2 में optional चौथे session के रूप में जोड़ा जा सकता है।

### Important edge cases

- session midnight cross करे
- Friday close से Sunday/Monday open तक weekend
- US और UK DST transition के बीच बदलता overlap
- phone timezone/date/time बदलना
- device reboot
- locale और 12/24-hour format
- market holidays: MVP में सामान्य schedule + disclaimer; बाद में holiday calendar/API

## Recommended Android architecture

- **Language:** Kotlin
- **Main UI:** Jetpack Compose + Material 3
- **Widget:** Jetpack Glance से शुरुआत; यदि second/minute-perfect live countdown चाहिए तो classic `RemoteViews`/`Chronometer` का छोटा proof-of-concept पहले करें
- **Date/time:** `java.time` (`ZoneId`, `ZonedDateTime`, `Duration`)
- **Preferences:** DataStore
- **Background work:** WorkManager केवल periodic reconciliation के लिए
- **Alerts:** AlarmManager exact boundary alerts के लिए, user opt-in के साथ; Android की exact-alarm permission/policy का ध्यान रखना होगा
- **No backend required:** session clock पूरी तरह offline चल सकती है

### Widget refresh reality

Android का normal `updatePeriodMillis` 30 मिनट से कम periodic update support नहीं करता। WorkManager का minimum periodic interval 15 मिनट है और Doze में execution देर से हो सकती है। हर minute पूरा widget redraw करना battery-friendly या reliable नहीं है।

इसलिए recommended strategy:

1. Widget content session boundary, timezone/time change, app interaction और periodic reconciliation पर update हो।
2. Countdown के लिए system-driven `Chronometer` feasibility test करें, ताकि हर second/minute app को जगाना न पड़े।
3. यदि launcher/Glance limitations के कारण exact countdown संभव न हो, widget पर absolute close time + “~4h 30m left” दिखाएँ और 15–30 मिनट refresh करें।
4. Main app खुला हो तो countdown हर second/minute exact update हो सकता है।

## Data/domain design

```kotlin
data class ForexSession(
    val id: String,
    val name: String,
    val zoneId: ZoneId,
    val localOpen: LocalTime,
    val localClose: LocalTime
)

data class SessionState(
    val isOpen: Boolean,
    val currentIntervalStart: Instant?,
    val currentIntervalEnd: Instant?,
    val nextOpen: Instant,
    val elapsed: Duration?,
    val remaining: Duration
)
```

Pure calculation engine को Android UI से अलग module/package में रखें। Tests में DST, weekends और midnight crossing cover करें।

## MVP scope

### Phase 1 — dependable core

- Tokyo, London, New York calculation engine
- automatic local timezone + DST
- weekday/weekend handling
- Compose dashboard and timeline
- unit tests around DST/weekends

### Phase 2 — Android widget

- small (next event), medium (3 sessions), large (timeline) layouts
- open/closed colors और remaining time
- tap-to-refresh / tap-to-open app
- widget pin prompt and settings

### Phase 3 — alerts and polish

- session/overlap notifications
- editable session definitions
- optional Sydney session
- localization: Hindi + English
- accessibility, dynamic color, AMOLED theme
- optional holiday data

## UX recommendations

- सिर्फ red/green पर निर्भर न रहें; dot + `OPEN/CLOSED` text रखें
- active overlap को amber/purple strip से highlight करें
- default screen सरल रहे; pairs/volatility जैसी जानकारी secondary screen में हो
- widget में seconds न दिखाएँ—minutes पर्याप्त और battery-friendly हैं
- “Best time to trade” के बजाय “Highest typical activity” लिखें; profit imply न करें
- notification defaults off रखें

## Questions to lock before implementation

1. “तीन sessions” से मतलब Tokyo, London और New York ही है?
2. Default language Hindi, English या bilingual हो?
3. Countdown minute-level (`2h 14m`) चाहिए या seconds भी?
4. सिर्फ APK/private use या Play Store release भी?
5. Alerts चाहिए? अगर हाँ, session open, overlap और close में से कौन-से?

## Research references

- Android Developers — Manage/update Glance widgets: https://developer.android.com/develop/ui/compose/glance/glance-app-widget
- Android Developers — Create a Glance app widget: https://developer.android.com/develop/ui/compose/glance/create-app-widget
- Android Developers — Advanced widget update guidance: https://developer.android.com/develop/ui/views/appwidgets/advanced
- TMGM — session hours and DST explanation: https://www.tmgm.com/en/academy/trading-academy/forex-market-hours
- Capital.com — session/overlap reference: https://capital.com/en-au/markets/forex/forex-market-trading-hours
