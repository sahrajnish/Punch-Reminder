package com.punchreminder.app.data

import androidx.room.TypeConverter
import com.punchreminder.app.domain.AttendanceState
import com.punchreminder.app.domain.ReminderKind

object Converters {

    @JvmStatic
    @TypeConverter
    fun stateToString(state: AttendanceState?): String? = state?.name

    @JvmStatic
    @TypeConverter
    fun stringToState(value: String?): AttendanceState? =
        value?.let { AttendanceState.valueOf(it) }

    @JvmStatic
    @TypeConverter
    fun kindToString(kind: ReminderKind?): String? = kind?.name

    @JvmStatic
    @TypeConverter
    fun stringToKind(value: String?): ReminderKind? =
        value?.let { ReminderKind.valueOf(it) }
}
