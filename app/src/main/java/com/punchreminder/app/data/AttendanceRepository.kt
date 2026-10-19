package com.punchreminder.app.data

import android.content.Context
import com.punchreminder.app.domain.AttendanceState
import com.punchreminder.app.domain.AttendanceSession
import com.punchreminder.app.domain.AttendanceStateMachine
import com.punchreminder.app.domain.ReminderKind
import com.punchreminder.app.domain.ReminderScheduler
import com.punchreminder.app.domain.SchedulingClock
import com.punchreminder.app.domain.SystemSchedulingClock
import com.punchreminder.app.domain.NextReminderCalculator
import com.punchreminder.app.domain.ReminderSettings
import com.punchreminder.app.domain.WorkdayCycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/** Outcome of a sign-in confirmation attempt. */
sealed class ConfirmSignInResult {
    object AlreadySignedIn : ConfirmSignInResult()
    data class SignedIn(val session: AttendanceSession) : ConfirmSignInResult()
}

/** Outcome of a sign-out confirmation attempt. */
sealed class ConfirmSignOutResult {
    object NotSignedIn : ConfirmSignOutResult()
    data class SignedOut(val session: AttendanceSession) : ConfirmSignOutResult()
}

/**
 * Coordinates attendance state, persistence, and reminder scheduling.
 *
 * The repository owns the scheduling decision (via [NextReminderCalculator]) and tells the
 * [ReminderScheduler] which alarms to set or cancel. This keeps the alarm manager thin and the
 * decision logic testable.
 */
class AttendanceRepository(
    private val dao: AttendanceDao,
    private val db: AttendanceDatabase,
    private val settingsRepository: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val stateMachine: AttendanceStateMachine,
    private val clock: SchedulingClock
) {

    private suspend fun readSettings(): AppSettings = settingsRepository.settings.first()

    /** Today's session, creating one if none exists yet. */
    suspend fun getOrCreateTodaySession(): AttendanceSession {
        return withContext(Dispatchers.IO) {
            val today = clock.today().toString()
            val entity = dao.findByWorkDate(today)
            val session = entity?.let { AttendanceSessionMapper.toDomain(it) }
                ?: AttendanceSession(workDate = today, state = AttendanceState.NOT_SIGNED_IN)
            if (entity == null) {
                val nowMillis = clock.now.toEpochMilli()
                val newEntity = AttendanceSessionMapper.toEntity(
                    session.copy(createdAt = nowMillis, updatedAt = nowMillis)
                )
                val id = dao.insert(newEntity)
                session.copy(id = id)
            } else session
        }
    }

    /** Today's session if one exists, null otherwise (no creation). */
    suspend fun findTodaysSession(): AttendanceSession? {
        return withContext(Dispatchers.IO) {
            val today = clock.today().toString()
            dao.findByWorkDate(today)?.let { AttendanceSessionMapper.toDomain(it) }
        }
    }

    /** Reactive stream: emits today's session whenever it changes in the database. */
    fun observeTodaysSession(): Flow<AttendanceSession?> =
        observeSession(clock.today().toString())

    /**
     * Reactive stream for the session on [workDate] (ISO yyyy-MM-dd). Unlike
     * [observeTodaysSession], the date is supplied by the caller, so the UI can switch to a new
     * day at midnight without recreating the ViewModel.
     */
    fun observeSession(workDate: String): Flow<AttendanceSession?> {
        return dao.observeByWorkDate(workDate).map { entity ->
            entity?.let { AttendanceSessionMapper.toDomain(it) }
        }
    }

    /** Reactive stream: emits all sessions whenever any session changes. */
    fun observeAllSessions(): Flow<List<AttendanceSession>> {
        return dao.observeAll().map { entities ->
            entities.map { AttendanceSessionMapper.toDomain(it) }
        }
    }

    /** All historical sessions, newest first. */
    suspend fun findAllSessions(): List<AttendanceSession> {
        return withContext(Dispatchers.IO) {
            dao.findAll().map { AttendanceSessionMapper.toDomain(it) }
        }
    }

    /**
     * Rebuild pending reminders from current state (boot / settings change / app open / midnight).
     *
     * Sessions left open on an earlier day are closed first (see [WorkdayCycle]): the cycle runs
     * 00:00–23:59, so a forgotten sign-out is recorded as [AttendanceState.NOT_SIGNED_OUT] at the
     * end of that work date instead of leaking into the new day.
     *
     * @return the number of previous-day sessions that were auto-closed by this call.
     */
    suspend fun rescheduleAll(): Int {
        return withContext(Dispatchers.IO) {
            val today = clock.today()
            val autoClosed = closePreviousDaySessions(today)

            val settings = readSettings().toReminderSettings()
            val now = clock.now
            val session = findTodaysSession() ?: AttendanceSession(workDate = today.toString())
            val plan = NextReminderCalculator.compute(now, today, settings, session)

            when (plan.action) {
                NextReminderCalculator.Plan.Action.CANCEL -> {
                    scheduler.cancelAll()
                    val existing = dao.findByWorkDate(today.toString())
                    if (existing != null) {
                        dao.update(
                            AttendanceSessionMapper.toEntity(
                                AttendanceSessionMapper.toDomain(existing).copy(
                                    nextReminderAt = null,
                                    nextReminderKind = null,
                                    updatedAt = now.toEpochMilli()
                                )
                            )
                        )
                    }
                }
                NextReminderCalculator.Plan.Action.SCHEDULE -> {
                    val kind = plan.kind!!
                    val fire = plan.fireAt!!
                    // Never let a reminder from the previous day survive the rollover.
                    val otherKind = if (kind == ReminderKind.SIGN_IN) {
                        ReminderKind.SIGN_OUT
                    } else {
                        ReminderKind.SIGN_IN
                    }
                    scheduler.cancel(otherKind)
                    scheduler.schedule(kind, fire)
                    val nowMillis = now.toEpochMilli()
                    val updated = session.copy(
                        nextReminderAt = fire.toEpochMilli(),
                        nextReminderKind = kind,
                        updatedAt = nowMillis
                    )
                    upsertSession(updated)
                }
            }
            autoClosed
        }
    }

    /**
     * Close every session still open (signed in / sign-out due) on a day before [today].
     * Returns how many were finalized. Each one is recorded as [AttendanceState.NOT_SIGNED_OUT]
     * with a sign-out instant at the end of its own work date.
     */
    private suspend fun closePreviousDaySessions(today: LocalDate): Int {
        val stale = dao.findOpenSessionsBefore(today.toString())
        if (stale.isEmpty()) return 0

        val nowMillis = clock.now.toEpochMilli()
        val zone = clock.zoneId
        stale.forEach { entity ->
            val session = AttendanceSessionMapper.toDomain(entity)
            upsertSession(WorkdayCycle.closeAtEndOfDay(session, zone, nowMillis))
        }
        return stale.size
    }

    /** The user has explicitly confirmed sign-in. */
    suspend fun confirmSignIn(): ConfirmSignInResult {
        return withContext(Dispatchers.IO) {
            val session = getOrCreateTodaySession()
            if (session.state != AttendanceState.NOT_SIGNED_IN) {
                ConfirmSignInResult.AlreadySignedIn
            } else {
                val now = clock.now
                val nowMillis = now.toEpochMilli()
                val settings = readSettings()
                val durationMillis =
                    settings.sessionDurationHours * 60L * 60L * 1000L +
                        settings.sessionDurationMinutes * 60L * 1000L
                val scheduledSignOutAt = nowMillis + durationMillis

                val updated = session.copy(
                    state = AttendanceState.SIGNED_IN,
                    confirmedSignInAt = nowMillis,
                    scheduledSignOutAt = scheduledSignOutAt,
                    nextReminderAt = scheduledSignOutAt,
                    nextReminderKind = ReminderKind.SIGN_OUT,
                    pauseReason = null,
                    updatedAt = nowMillis
                )
                upsertSession(updated)

                scheduler.cancel(ReminderKind.SIGN_IN)

                // If the calculated sign-out time is already in the past, schedule it
                // immediately so the transition to SIGN_OUT_DUE happens promptly.
                if (scheduledSignOutAt <= nowMillis) {
                    scheduler.schedule(ReminderKind.SIGN_OUT, clock.now.plusSeconds(5))
                } else {
                    scheduler.schedule(ReminderKind.SIGN_OUT, Instant.ofEpochMilli(scheduledSignOutAt))
                }

                ConfirmSignInResult.SignedIn(updated)
            }
        }
    }

    /** The user has explicitly confirmed sign-out. */
    suspend fun confirmSignOut(): ConfirmSignOutResult {
        return withContext(Dispatchers.IO) {
            val session = getOrCreateTodaySession()
            val valid = session.state == AttendanceState.SIGNED_IN ||
                session.state == AttendanceState.SIGN_OUT_DUE
            if (!valid) {
                ConfirmSignOutResult.NotSignedIn
            } else {
                val nowMillis = clock.now.toEpochMilli()
                val updated = session.copy(
                    state = AttendanceState.SIGNED_OUT,
                    confirmedSignOutAt = nowMillis,
                    nextReminderAt = null,
                    nextReminderKind = null,
                    pauseReason = null,
                    updatedAt = nowMillis
                )
                upsertSession(updated)
                scheduler.cancelAll()
                ConfirmSignOutResult.SignedOut(updated)
            }
        }
    }

    /** Snooze the sign-in reminder by [minutes] (moves the next reminder only). */
    suspend fun snoozeSignIn(minutes: Int) {
        withContext(Dispatchers.IO) {
            val session = getOrCreateTodaySession()
            if (session.state != AttendanceState.NOT_SIGNED_IN) return@withContext
            val nowMillis = clock.now.toEpochMilli()
            val next = nowMillis + minutes.toLong() * 60 * 1000
            val updated = session.copy(
                nextReminderAt = next,
                nextReminderKind = ReminderKind.SIGN_IN,
                updatedAt = nowMillis
            )
            upsertSession(updated)
            scheduler.cancel(ReminderKind.SIGN_IN)
            scheduler.schedule(ReminderKind.SIGN_IN, clock.now.plusSeconds(minutes.toLong() * 60))
        }
    }

    /** Snooze the sign-out reminder by [minutes] (moves the next reminder only). */
    suspend fun snoozeSignOut(minutes: Int) {
        withContext(Dispatchers.IO) {
            val session = getOrCreateTodaySession()
            if (session.state != AttendanceState.SIGN_OUT_DUE) return@withContext
            val nowMillis = clock.now.toEpochMilli()
            val next = nowMillis + minutes.toLong() * 60 * 1000
            val updated = session.copy(
                nextReminderAt = next,
                nextReminderKind = ReminderKind.SIGN_OUT,
                updatedAt = nowMillis
            )
            upsertSession(updated)
            scheduler.cancel(ReminderKind.SIGN_OUT)
            scheduler.schedule(ReminderKind.SIGN_OUT, clock.now.plusSeconds(minutes.toLong() * 60))
        }
    }

    /**
     * Handle a sign-out reminder alarm firing.
     * @return true when a sign-out notification should be shown to the user.
     */
    internal suspend fun handleSignOutAlarmFired(): Boolean {
        return withContext(Dispatchers.IO) {
            val session = findTodaysSession()
            if (session == null) {
                // The alarm crossed into a new day. Rebuild today's schedule instead of stopping.
                rescheduleAll()
                return@withContext false
            }

            val settings = readSettings()
            val now = clock.now
            val nowMillis = now.toEpochMilli()
            val next = nowMillis + settings.repeatIntervalMinutes.toLong() * 60 * 1000

            val updated = when (session.state) {
                AttendanceState.SIGNED_IN -> {
                    session.copy(
                        state = AttendanceState.SIGN_OUT_DUE,
                        nextReminderAt = next,
                        nextReminderKind = ReminderKind.SIGN_OUT,
                        updatedAt = nowMillis
                    )
                }
                AttendanceState.SIGN_OUT_DUE -> {
                    session.copy(
                        nextReminderAt = next,
                        nextReminderKind = ReminderKind.SIGN_OUT,
                        updatedAt = nowMillis
                    )
                }
                else -> return@withContext false
            }
            upsertSession(updated)
            scheduler.cancel(ReminderKind.SIGN_OUT)
            scheduler.schedule(
                ReminderKind.SIGN_OUT,
                now.plusSeconds(settings.repeatIntervalMinutes.toLong() * 60)
            )
            true
        }
    }

    /**
     * Handle a sign-in reminder alarm firing.
     * @return true when a sign-in notification should be shown to the user.
     */
    internal suspend fun handleSignInAlarmFired(): Boolean {
        return withContext(Dispatchers.IO) {
            val session = findTodaysSession()
            if (session == null) {
                // The alarm crossed into a new day. Rebuild today's schedule instead of stopping.
                rescheduleAll()
                return@withContext false
            }
            if (session.state != AttendanceState.NOT_SIGNED_IN) return@withContext false
            val settings = readSettings()
            val nowMillis = clock.now.toEpochMilli()
            val next = nowMillis + settings.repeatIntervalMinutes.toLong() * 60 * 1000
            val updated = session.copy(
                nextReminderAt = next,
                nextReminderKind = ReminderKind.SIGN_IN,
                updatedAt = nowMillis
            )
            upsertSession(updated)
            scheduler.cancel(ReminderKind.SIGN_IN)
            scheduler.schedule(ReminderKind.SIGN_IN, clock.now.plusSeconds(settings.repeatIntervalMinutes.toLong() * 60))
            true
        }
    }

    /** Delete all local attendance history (user action, with confirmation in UI). */
    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            dao.deleteAll()
            scheduler.cancelAll()
        }
    }

    /** Delete a single session by work date. */
    suspend fun deleteSession(workDate: String) {
        withContext(Dispatchers.IO) {
            dao.deleteByWorkDate(workDate)
            if (workDate == clock.today().toString()) {
                // Today's session was removed deliberately. rescheduleAll() would immediately
                // upsert a fresh session for today, so cancel reminders instead.
                scheduler.cancelAll()
            } else {
                rescheduleAll()
            }
        }
    }

    /** Pause today's reminders with an optional reason (PTO, holiday, etc.). Resets tomorrow. */
    suspend fun skipToday(reason: String? = null) {
        withContext(Dispatchers.IO) {
            val session = findTodaysSession()
                ?: AttendanceSession(workDate = clock.today().toString())
            val nowMillis = clock.now.toEpochMilli()
            val updated = session.copy(
                state = AttendanceState.SKIPPED,
                nextReminderAt = null,
                nextReminderKind = null,
                pauseReason = reason,
                updatedAt = nowMillis
            )
            upsertSession(updated)
            scheduler.cancelAll()
        }
    }

    /** Undo a pause for today and rebuild the day's reminder schedule. */
    suspend fun resumeToday() {
        withContext(Dispatchers.IO) {
            val session = findTodaysSession() ?: return@withContext
            if (session.state != AttendanceState.SKIPPED) return@withContext
            val nowMillis = clock.now.toEpochMilli()
            val updated = session.copy(
                state = AttendanceState.NOT_SIGNED_IN,
                pauseReason = null,
                nextReminderAt = null,
                nextReminderKind = null,
                updatedAt = nowMillis
            )
            upsertSession(updated)
            rescheduleAll()
        }
    }

    private suspend fun upsertSession(session: AttendanceSession) {
        val entity = AttendanceSessionMapper.toEntity(session)
        val existing = dao.findByWorkDate(session.workDate)
        if (existing == null) {
            dao.insert(entity.copy(id = 0))
        } else {
            dao.update(entity.copy(id = existing.id))
        }
    }

    companion object {
        fun create(
            context: Context,
            scheduler: ReminderScheduler,
            settingsRepository: SettingsRepository
        ): AttendanceRepository {
            val db = AttendanceDatabase.getDatabase(context)
            return AttendanceRepository(
                dao = db.attendanceDao(),
                db = db,
                settingsRepository = settingsRepository,
                scheduler = scheduler,
                stateMachine = AttendanceStateMachine(),
                clock = SystemSchedulingClock
            )
        }
    }
}
