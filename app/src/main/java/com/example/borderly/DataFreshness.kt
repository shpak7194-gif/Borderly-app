package com.example.borderly

import android.app.ActivityManager
import android.content.Context
import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Samsung M21-class devices (4GB RAM, weak GPU) get memoryClass <= 192.
// Backdrop blur and other heavy effects are disabled there to keep the UI smooth.
internal fun isBorderlyLowEndDevice(context: Context): Boolean {
    val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false
    return activityManager.isLowRamDevice || activityManager.memoryClass <= 192
}

internal fun formatDataDateForUi(
    raw: String,
    locale: Locale = Locale.getDefault()
): String {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
    }
    val position = ParsePosition(0)
    val parsed = parser.parse(raw, position)
        ?.takeIf { position.index == raw.length && parser.format(it) == raw }
        ?: return raw

    return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(parsed)
}

internal fun formatLastSuccessfulCheckForUi(
    timestamp: Long,
    locale: Locale = Locale.getDefault()
): String {
    if (timestamp <= 0L) return "—"

    val checked = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val now = Calendar.getInstance()

    val sameDay =
        checked.get(Calendar.ERA) == now.get(Calendar.ERA) &&
        checked.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        checked.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

    val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        .format(Date(timestamp))

    return if (sameDay) {
        time
    } else {
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            .format(Date(timestamp))
        "$date, $time"
    }
}
