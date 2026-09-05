package com.forex.sessionclock.domain
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class SessionEngineTest {
 @Test fun tokyoOpenOnWeekday(){val s=SessionEngine.state(SessionEngine.sessions[0],Instant.parse("2026-09-07T01:00:00Z"));assertTrue(s.isOpen);assertEquals(8*3600,s.remaining.seconds)}
 @Test fun tokyoSkipsWeekend(){val s=SessionEngine.state(SessionEngine.sessions[0],Instant.parse("2026-09-05T01:00:00Z"));assertFalse(s.isOpen);assertEquals(DayOfWeek.MONDAY,s.nextOpen.atZone(ZoneId.of("Asia/Tokyo")).dayOfWeek)}
 @Test fun londonDstIsApplied(){val london=SessionEngine.sessions[1];val summer=SessionEngine.state(london,Instant.parse("2026-07-06T07:30:00Z"));val winter=SessionEngine.state(london,Instant.parse("2026-01-05T08:30:00Z"));assertTrue(summer.isOpen);assertTrue(winter.isOpen)}
 @Test fun overlapEventExists(){val events=SessionEngine.upcomingEvents(Instant.parse("2026-09-07T00:00:00Z"),2);assertTrue(events.any{it.type==EventType.OVERLAP})}
}
