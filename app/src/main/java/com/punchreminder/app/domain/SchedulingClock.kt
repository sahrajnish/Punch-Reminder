package com.punchreminder.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * An injectable clock so scheduling logic can be tested deterministically without real waiting.
 */
interface SchedulingClock {
    val now: Instant
    val timeZone: TimeZone
    val zoneId: ZoneId get() = ZoneId.of(timeZone.id)
    fun today(): LocalDate
    fun localDateTime(instant: Instant): LocalDateTime
}

/** Backed by the device wall clock and default time zone. */
object SystemSchedulingClock : SchedulingClock {
    override val now: Instant get() = Instant.now()
    override val timeZone: TimeZone get() = TimeZone.getDefault()
    override fun today(): LocalDate = LocalDate.now(zoneId)
    override fun localDateTime(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, zoneId)
}

/** A fixed clock for unit tests. */
class TestSchedulingClock(
    override val now: Instant,
    override val timeZone: TimeZone = TimeZone.getTimeZone("UTC")
) : SchedulingClock {
    override fun today(): LocalDate = now.atZone(zoneId).toLocalDate()
    override fun localDateTime(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, zoneId)
}
