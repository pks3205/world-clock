package com.forex.sessionclock.domain

import java.time.*

data class ForexSession(val id: String, val name: String, val city: String, val zone: ZoneId, val open: LocalTime, val close: LocalTime, val color: Long)
data class SessionState(val session: ForexSession, val isOpen: Boolean, val start: Instant?, val end: Instant?, val nextOpen: Instant, val now: Instant) {
    val target: Instant get() = if (isOpen) end!! else nextOpen
    val remaining: Duration get() = Duration.between(now, target).coerceAtLeast(Duration.ZERO)
    val elapsed: Duration? get() = start?.let { Duration.between(it, now).coerceAtLeast(Duration.ZERO) }
}
data class MarketEvent(val id: String, val title: String, val body: String, val instant: Instant, val type: EventType)
enum class EventType { OPEN, CLOSE, OVERLAP }

object SessionEngine {
    val sessions = listOf(
        ForexSession("asia", "ASIA", "Tokyo", ZoneId.of("Asia/Tokyo"), LocalTime.of(9,0), LocalTime.of(18,0), 0xFF36BFFA),
        ForexSession("london", "LONDON", "London", ZoneId.of("Europe/London"), LocalTime.of(8,0), LocalTime.of(17,0), 0xFFB692F6),
        ForexSession("new_york", "NEW YORK", "New York", ZoneId.of("America/New_York"), LocalTime.of(8,0), LocalTime.of(17,0), 0xFFFFB84D)
    )

    fun states(now: Instant = Instant.now()): List<SessionState> = sessions.map { state(it, now) }

    fun state(s: ForexSession, now: Instant): SessionState {
        val localNow = now.atZone(s.zone)
        val candidates = (-3L..8L).map { offset -> interval(s, localNow.toLocalDate().plusDays(offset)) }
            .filter { it.first.atZone(s.zone).dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
        val current = candidates.firstOrNull { !now.isBefore(it.first) && now.isBefore(it.second) }
        val next = candidates.first { it.first.isAfter(now) }
        return SessionState(s, current != null, current?.first, current?.second, next.first, now)
    }

    private fun interval(s: ForexSession, date: LocalDate): Pair<Instant, Instant> {
        val start = ZonedDateTime.of(date, s.open, s.zone)
        val endDate = if (s.close <= s.open) date.plusDays(1) else date
        return start.toInstant() to ZonedDateTime.of(endDate, s.close, s.zone).toInstant()
    }

    fun upcomingEvents(now: Instant = Instant.now(), days: Int = 8): List<MarketEvent> {
        val events = mutableListOf<MarketEvent>()
        sessions.forEach { s ->
            val localDate = now.atZone(s.zone).toLocalDate()
            (0L..days.toLong()).forEach { d ->
                val date = localDate.plusDays(d)
                if (date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                    val (start, end) = interval(s, date)
                    if (start.isAfter(now)) events += MarketEvent("${s.id}-open-$date", "${s.name} session opened", "${s.city} session is live now", start, EventType.OPEN)
                    if (end.isAfter(now)) events += MarketEvent("${s.id}-close-$date", "${s.name} session closed", "${s.city} session has ended", end, EventType.CLOSE)
                }
            }
        }
        // Calculate actual London/New York intersections so mixed DST weeks are correct.
        val london = sessions[1]; val ny = sessions[2]
        val londonDate = now.atZone(london.zone).toLocalDate()
        (0L..days.toLong()).forEach { d ->
            val date = londonDate.plusDays(d)
            if (date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                val li = interval(london, date)
                (-1L..1L).forEach { nyd ->
                    val ni = interval(ny, date.plusDays(nyd))
                    val start = maxOf(li.first, ni.first); val end = minOf(li.second, ni.second)
                    if (start < end && start.isAfter(now)) events += MarketEvent("overlap-$date", "London + New York overlap", "The highest-liquidity window has started", start, EventType.OVERLAP)
                }
            }
        }
        return events.distinctBy { it.id }.sortedBy { it.instant }
    }
}

fun Duration.clock(): String {
    val total = seconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}
