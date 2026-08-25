package com.example.borderly

import android.app.ActivityManager
import android.content.Context
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

internal fun formatDataDateForUi(raw: String): String {
    val parts = raw.split("-")
    return if (
        parts.size == 3 &&
        parts[0].length == 4 &&
        parts[1].length == 2 &&
        parts[2].length == 2
    ) {
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } else {
        raw
    }
}

internal fun formatLastSuccessfulCheckForUi(timestamp: Long): String {
    if (timestamp <= 0L) return "ещё не выполнялась"

    val checked = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val now = Calendar.getInstance()

    val sameDay =
        checked.get(Calendar.ERA) == now.get(Calendar.ERA) &&
        checked.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        checked.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(timestamp))

    return if (sameDay) {
        "сегодня, $time"
    } else {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            .format(Date(timestamp))
        "$date, $time"
    }
}
