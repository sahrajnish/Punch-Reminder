package com.punchreminder.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters as RoomTypeConverters
import com.punchreminder.app.domain.AttendanceSession
import com.punchreminder.app.domain.AttendanceState
import com.punchreminder.app.domain.ReminderKind

@Database(entities = [AttendanceSessionEntity::class], version = 1)
@RoomTypeConverters(Converters::class)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile private var INSTANCE: AttendanceDatabase? = null

        fun getDatabase(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "punch_reminder_database"
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

/** Maps between the domain model and the Room entity. */
object AttendanceSessionMapper {

    fun toEntity(session: AttendanceSession): AttendanceSessionEntity = AttendanceSessionEntity(
        id = session.id,
        workDate = session.workDate,
        state = session.state.name,
        confirmedSignInAt = session.confirmedSignInAt,
        confirmedSignOutAt = session.confirmedSignOutAt,
        scheduledSignOutAt = session.scheduledSignOutAt,
        nextReminderAt = session.nextReminderAt,
        nextReminderKind = session.nextReminderKind?.name,
        createdAt = session.createdAt,
        updatedAt = session.updatedAt
    )

    fun toDomain(entity: AttendanceSessionEntity): AttendanceSession = AttendanceSession(
        id = entity.id,
        workDate = entity.workDate,
        state = AttendanceState.valueOf(entity.state),
        confirmedSignInAt = entity.confirmedSignInAt,
        confirmedSignOutAt = entity.confirmedSignOutAt,
        scheduledSignOutAt = entity.scheduledSignOutAt,
        nextReminderAt = entity.nextReminderAt,
        nextReminderKind = entity.nextReminderKind?.let { ReminderKind.valueOf(it) },
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )
}
