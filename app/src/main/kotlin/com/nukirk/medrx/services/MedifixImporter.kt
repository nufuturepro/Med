package com.nukirk.medrx.services

import android.content.Context
import com.nukirk.medrx.ItemType
import com.nukirk.medrx.R
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Imports data exported by MediFix (formatVersion 1 JSON exports containing
 * medications, schedules and intakeLogs) into Med's MedData model.
 *
 * Mapping notes:
 * - Each schedule becomes one MedData group (shared groupId); each daily time
 *   becomes its own MedData entry, exactly like "add medicine with multiple times".
 * - Every UUID is mapped to a stable Long id, so re-importing the same file is
 *   idempotent: existing entries are matched and only missing history is filled.
 * - Intake logs are merged into takenHistory keyed by the planned day, keeping
 *   the earliest actual time per day (Med stores one time per day).
 * - Paused medicines are imported with notifications disabled (notificationType 1);
 *   critical alarms map to notificationType 3; everything else follows app defaults.
 * - Archived medicines are skipped.
 * - Dosage, active ingredient and prescriber have no dedicated fields in Med:
 *   dosage goes into the title, the rest into notes.
 */
object MedifixImporter {

    fun isMedifixExport(root: JSONObject): Boolean {
        return root.has("medications") && root.has("schedules") && root.has("intakeLogs")
    }

    private data class ScheduleInfo(
        val id: String,
        val medId: String,
        val times: List<LocalTime>,
        val weekdays: List<DayOfWeek>?,
        val intervalDays: Int?,
        val startDate: LocalDateTime?,
        val endDate: LocalDate?
    )

    fun parse(context: Context, root: JSONObject): List<MedData> {
        val medsJson = root.optJSONArray("medications") ?: return emptyList()
        val today = LocalDate.now()

        val schedules = parseSchedules(root.optJSONArray("schedules"))
        val logs = parseIntakeLogs(root.optJSONArray("intakeLogs"), schedules, today)

        val items = mutableListOf<MedData>()
        for (i in 0 until medsJson.length()) {
            val med = try {
                medsJson.getJSONObject(i)
            } catch (e: Exception) {
                continue
            }

            try {
                val medId = med.optString("id")
                val name = optClean(med, "name")
                if (medId.isBlank() || name.isBlank()) continue
                if (med.optBoolean("isArchived", false)) continue

                val paused = med.optBoolean("isPaused", false)
                val critical = med.optBoolean("isCriticalAlarm", false)
                val notificationType = when {
                    paused -> 1      // none
                    critical -> 3    // alarm
                    else -> 0        // follow global settings
                }
                val dosage = optClean(med, "dosage")
                val notes = buildNotes(med)
                val createdAt = parseDateTime(med.optString("createdAt"))
                val title = buildTitle(name, dosage)

                val medSchedules = schedules.filter { it.medId == medId }
                if (medSchedules.isEmpty()) {
                    items.add(
                        MedData(
                            id = stableLongId(medId),
                            groupId = null,
                            type = ItemType.Medicine,
                            title = title,
                            iconName = null,
                            colorCode = null,
                            frequencyLabel = "",
                            creationDate = createdAt?.toLocalDate() ?: today,
                            creationTime = createdAt?.toLocalTime() ?: LocalTime.NOON,
                            takenHistory = bucketFor(logs, "med:$medId", createdAt?.toLocalTime() ?: LocalTime.NOON),
                            recurrenceDays = null,
                            endDate = null,
                            notes = notes,
                            displayOrder = i,
                            intervalGap = null,
                            category = null,
                            notificationType = notificationType
                        )
                    )
                } else {
                    medSchedules.forEach { schedule ->
                        val groupId = stableLongId(schedule.id)
                        val created = schedule.startDate ?: createdAt
                        val medTimes = schedule.times
                        medTimes.forEachIndexed { ti, time ->
                            items.add(
                                MedData(
                                    id = stableLongId(schedule.id) + ti,
                                    groupId = groupId,
                                    type = ItemType.Medicine,
                                    title = title,
                                    iconName = null,
                                    colorCode = null,
                                    frequencyLabel = buildFrequencyLabel(
                                        context, dosage, medTimes.size, schedule.weekdays, schedule.intervalDays
                                    ),
                                    creationDate = created?.toLocalDate() ?: today,
                                    creationTime = time,
                                    takenHistory = bucketFor(logs, schedule.id, time),
                                    recurrenceDays = schedule.weekdays,
                                    endDate = schedule.endDate,
                                    notes = notes,
                                    displayOrder = i,
                                    intervalGap = schedule.intervalDays,
                                    category = null,
                                    notificationType = notificationType
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        return items
    }

    private fun parseSchedules(arr: JSONArray?): List<ScheduleInfo> {
        if (arr == null) return emptyList()
        val result = mutableListOf<ScheduleInfo>()
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                val times = parseStringArray(o.opt("timesOfDay"))
                    .mapNotNull { parseTime(it) }
                if (times.isEmpty()) continue
                val weekdayInts = parseStringArray(o.opt("weekdays"))
                    .mapNotNull { it.trim().toIntOrNull() }
                result.add(
                    ScheduleInfo(
                        id = o.optString("id"),
                        medId = o.optString("medicationId"),
                        times = times,
                        weekdays = if (weekdayInts.isEmpty()) null else weekdayInts.mapNotNull { toDayOfWeek(it) },
                        intervalDays = if (o.isNull("intervalDays")) null else o.optInt("intervalDays").takeIf { it > 0 },
                        startDate = parseDateTime(o.optString("startDate")),
                        endDate = parseDateTime(o.optString("endDate"))?.toLocalDate()
                    )
                )
            } catch (e: Exception) {
                continue
            }
        }
        return result
    }

    /**
     * Returns raw logs keyed by scheduleId (or "med:<id>" for logs without a matching
     * schedule), as (day, actual time) pairs.
     */
    private fun parseIntakeLogs(
        arr: JSONArray?,
        schedules: List<ScheduleInfo>,
        today: LocalDate
    ): HashMap<String, MutableList<Pair<LocalDate, LocalTime>>> {
        val logs = HashMap<String, MutableList<Pair<LocalDate, LocalTime>>>()
        if (arr == null) return logs
        val scheduleMedIds = schedules.associate { it.id to it.medId }

        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                if (!o.optString("status").equals("TAKEN", ignoreCase = true)) continue

                val planned = parseDateTime(o.optString("plannedAt"))
                val actual = parseDateTime(o.optString("actualAt")) ?: planned ?: continue
                val date = planned?.toLocalDate() ?: actual.toLocalDate()
                if (date.isAfter(today)) continue

                val scheduleId = if (o.isNull("scheduleId")) null else o.optString("scheduleId")
                val medId = o.optString("medicationId")
                val key = when {
                    scheduleId != null && scheduleMedIds.containsKey(scheduleId) -> scheduleId
                    medId.isNotBlank() -> "med:$medId"
                    else -> continue
                }
                logs.getOrPut(key) { mutableListOf() }.add(date to actual.toLocalTime())
            } catch (e: Exception) {
                continue
            }
        }
        return logs
    }

    /**
     * Builds the takenHistory for one time-slot: per day, the log whose time is
     * closest to the slot's time wins (Med stores one time per day per entry).
     */
    private fun bucketFor(
        logs: HashMap<String, MutableList<Pair<LocalDate, LocalTime>>>,
        key: String,
        slotTime: LocalTime
    ): HashMap<LocalDate, LocalTime> {
        val dayLogs = logs[key] ?: return HashMap()
        val bucket = HashMap<LocalDate, LocalTime>()
        dayLogs.forEach { (day, time) ->
            val existing = bucket[day]
            if (existing == null || absMinutesBetween(time, slotTime) < absMinutesBetween(existing, slotTime)) {
                bucket[day] = time
            }
        }
        return bucket
    }

    private fun absMinutesBetween(a: LocalTime, b: LocalTime): Long {
        return Math.abs(ChronoUnit.MINUTES.between(a, b))
    }

    private fun buildTitle(name: String, dosage: String): String {
        return if (dosage.isBlank() || name.contains(dosage, ignoreCase = true)) name
        else "$name – $dosage"
    }

    private fun buildNotes(med: JSONObject): String? {
        val lines = mutableListOf<String>()
        val ingredient = optClean(med, "activeIngredient")
        val prescriber = optClean(med, "prescriber")
        val notes = optClean(med, "notes")
        if (ingredient.isNotBlank()) lines.add("Active ingredient: $ingredient")
        if (prescriber.isNotBlank()) lines.add("Prescriber: $prescriber")
        if (notes.isNotBlank()) lines.add(notes)
        return if (lines.isEmpty()) null else lines.joinToString("\n")
    }

    private fun buildFrequencyLabel(
        context: Context,
        dosage: String,
        timesCount: Int,
        weekdays: List<DayOfWeek>?,
        intervalDays: Int?
    ): String {
        val freq = when {
            intervalDays != null -> context.getString(R.string.frequency_every_days, intervalDays)
            weekdays != null -> context.getString(R.string.frequency_specific_days)
            timesCount > 1 -> context.getString(R.string.frequency_daily_multiple, timesCount)
            else -> context.getString(R.string.frequency_daily)
        }
        return if (dosage.isBlank()) freq else "$dosage · $freq"
    }

    /** MediFix weekdays follow ISO numbering (1 = Monday, 7 = Sunday); 0 maps defensively to Sunday. */
    private fun toDayOfWeek(value: Int): DayOfWeek? = when (value) {
        in 1..7 -> DayOfWeek.of(value)
        0 -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun parseStringArray(value: Any?): List<String> {
        return when (value) {
            is String -> try {
                val arr = JSONArray(value)
                (0 until arr.length()).map { arr.optString(it) }
            } catch (e: Exception) {
                emptyList()
            }

            is JSONArray -> (0 until value.length()).map { value.optString(it) }
            else -> emptyList()
        }
    }

    private fun parseTime(raw: String): LocalTime? = try {
        LocalTime.parse(raw.trim())
    } catch (e: Exception) {
        null
    }

    private fun parseDateTime(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank() || raw == "null") return null
        try {
            return LocalDateTime.parse(raw)
        } catch (e: Exception) {
        }
        try {
            return Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } catch (e: Exception) {
        }
        // Last resort: date-only string.
        if (raw.length >= 10) {
            try {
                return LocalDate.parse(raw.substring(0, 10)).atTime(LocalTime.NOON)
            } catch (e: Exception) {
            }
        }
        return null
    }

    /** org.json coerces JSON nulls to the literal string "null" — normalize those away. */
    private fun optClean(obj: JSONObject, key: String): String {
        return obj.optString(key).takeUnless { it.isBlank() || it == "null" } ?: ""
    }

    /** Deterministic Long id from a UUID string, so re-imports match existing entries. */
    private fun stableLongId(uuid: String?): Long {
        if (uuid.isNullOrBlank() || uuid == "null") return System.currentTimeMillis()
        return try {
            UUID.fromString(uuid).mostSignificantBits
        } catch (e: Exception) {
            uuid.hashCode().toLong()
        }
    }
}
