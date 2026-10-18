package com.punchreminder.app.domain

import java.time.DayOfWeek

/**
 * Encodes the weekly repeat cycle as a bit mask:
 *   bit 0 = Monday ... bit 5 = Saturday, bit 6 = Sunday.
 */
object Workdays {
    const val MONDAY = 1
    const val TUESDAY = 1 shl 1
    const val WEDNESDAY = 1 shl 2
    const val THURSDAY = 1 shl 3
    const val FRIDAY = 1 shl 4
    const val SATURDAY = 1 shl 5
    const val SUNDAY = 1 shl 6

    /** Monday through Friday (the app default). */
    val DEFAULT_WORKDAYS: Int
        get() = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY

    fun isWorkday(bits: Int, dayOfWeek: DayOfWeek): Boolean {
        val bit = when (dayOfWeek) {
            DayOfWeek.MONDAY -> MONDAY
            DayOfWeek.TUESDAY -> TUESDAY
            DayOfWeek.WEDNESDAY -> WEDNESDAY
            DayOfWeek.THURSDAY -> THURSDAY
            DayOfWeek.FRIDAY -> FRIDAY
            DayOfWeek.SATURDAY -> SATURDAY
            DayOfWeek.SUNDAY -> SUNDAY
        }
        return (bits and bit) != 0
    }
}
