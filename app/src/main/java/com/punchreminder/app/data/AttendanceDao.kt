package com.punchreminder.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(session: AttendanceSessionEntity): Long

    @Update
    fun update(session: AttendanceSessionEntity): Int

    /** Upsert: insert or update by work date. */
    fun upsert(session: AttendanceSessionEntity): Long {
        val id = insert(session)
        return if (id > 0) id else updateOrInsertByDate(session)
    }

    private fun updateOrInsertByDate(session: AttendanceSessionEntity): Long {
        // This helper is invoked only when insert IGNORE skipped an existing row.
        throw UnsupportedOperationException("Use upsertSession instead")
    }

    @Query("SELECT * FROM attendance_sessions WHERE work_date = :workDate LIMIT 1")
    fun findByWorkDate(workDate: String): AttendanceSessionEntity?

    /** Reactive stream: emits whenever today's session changes. */
    @Query("SELECT * FROM attendance_sessions WHERE work_date = :workDate LIMIT 1")
    fun observeByWorkDate(workDate: String): Flow<AttendanceSessionEntity?>

    @Query("SELECT * FROM attendance_sessions ORDER BY work_date DESC")
    fun findAll(): List<AttendanceSessionEntity>

    /** Reactive stream: emits whenever any session changes. */
    @Query("SELECT * FROM attendance_sessions ORDER BY work_date DESC")
    fun observeAll(): Flow<List<AttendanceSessionEntity>>

    /**
     * Sessions from an earlier day that were never finished (signed in without a sign-out).
     * These are auto-closed at the end of their work date during the daily rollover.
     */
    @Query(
        """
        SELECT * FROM attendance_sessions
        WHERE work_date < :today
          AND state IN ('SIGNED_IN', 'SIGN_OUT_DUE')
        """
    )
    fun findOpenSessionsBefore(today: String): List<AttendanceSessionEntity>

    @Query("DELETE FROM attendance_sessions WHERE work_date = :workDate")
    fun deleteByWorkDate(workDate: String): Int

    @Query("DELETE FROM attendance_sessions")
    fun deleteAll(): Int
}
