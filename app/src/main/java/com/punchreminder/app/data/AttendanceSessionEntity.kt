package com.punchreminder.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.punchreminder.app.domain.AttendanceState
import com.punchreminder.app.domain.ReminderKind

@Entity(
    tableName = "attendance_sessions",
    indices = [Index(value = ["work_date"], unique = true)]
)
data class AttendanceSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "work_date") val workDate: String,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "confirmed_sign_in_at") val confirmedSignInAt: Long?,
    @ColumnInfo(name = "confirmed_sign_out_at") val confirmedSignOutAt: Long?,
    @ColumnInfo(name = "scheduled_sign_out_at") val scheduledSignOutAt: Long?,
    @ColumnInfo(name = "next_reminder_at") val nextReminderAt: Long?,
    @ColumnInfo(name = "next_reminder_kind") val nextReminderKind: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
