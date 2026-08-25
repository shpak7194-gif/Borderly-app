package com.example.borderly

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

internal const val RemoteEntryRequirementVersionUrl =
    "https://shpak7194-gif.github.io/borderly-data/entry_requirements_version.json"

internal const val EntryRequirementCacheFileName = "entry_requirements_remote.json"
internal const val EntryRequirementCachePreviousFileName = "entry_requirements_previous.json"
internal const val RemoteEntryRequirementVersionPreference = "remote_entry_requirements_version"
internal const val RemoteEntryRequirementLastCheckPreference = "remote_entry_requirements_last_check"
internal const val EntryRequirementUpdateCheckIntervalMs = 24L * 60L * 60L * 1000L
internal const val RemoteEntryRequirementDatabaseMaxBytes = 2 * 1024 * 1024

internal fun loadEntryRequirementDatabase(context: Context): EntryRequirementDatabase {
    val preferences =
        context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
    val lastSuccessfulCheckAt =
        preferences.getLong(RemoteEntryRequirementLastCheckPreference, 0L)
    val cachedFile = File(context.filesDir, EntryRequirementCacheFileName)
    val previousFile = File(context.filesDir, EntryRequirementCachePreviousFileName)
    val bundled = parseEntryRequirementDatabase(
        text = bundledEntryRequirementDatabaseText(context),
        origin = VisaDatabaseOrigin.BUNDLED,
        expectedVersion = null,
        lastSuccessfulCheckAt = lastSuccessfulCheckAt
    )
    val candidates = mutableListOf(bundled)
    listOf(cachedFile, previousFile).forEach { file ->
        if (!file.isFile) return@forEach
        runCatching {
            candidates += parseEntryRequirementDatabase(
                text = file.readText(Charsets.UTF_8),
                origin = VisaDatabaseOrigin.REMOTE,
                expectedVersion = null,
                lastSuccessfulCheckAt = lastSuccessfulCheckAt
            )
        }.onFailure { file.delete() }
    }
    return candidates.maxWithOrNull(
        compareBy<EntryRequirementDatabase> { it.version }
            .thenBy { if (it.origin == VisaDatabaseOrigin.REMOTE) 1 else 0 }
    ) ?: bundled
}

internal fun bundledEntryRequirementDatabaseText(context: Context): String =
    context.assets.open("entry_requirements.json")
        .bufferedReader()
        .use { it.readText() }

internal fun parseEntryRequirementDatabase(
    text: String,
    origin: VisaDatabaseOrigin,
    expectedVersion: Int?,
    lastSuccessfulCheckAt: Long
): EntryRequirementDatabase {
    val root = JSONObject(text)
    require(root.optInt("schemaVersion", 0) == 1) {
        "entry_requirements.json: unsupported schemaVersion"
    }

    val version = root.optInt("version", 0)
    require(version > 0) {
        "entry_requirements.json: invalid version"
    }
    if (expectedVersion != null) {
        require(version == expectedVersion) {
            "entry_requirements.json: version mismatch"
        }
    }

    val updated = requiredEntryRequirementText(root, "updated", 32)
    val array = root.optJSONArray("requirements")
        ?: error("entry_requirements.json: missing requirements array")
    require(array.length() in 1..5000) {
        "entry_requirements.json: invalid requirement count"
    }

    val ids = mutableSetOf<String>()
    val grouped = LinkedHashMap<Pair<Int, Int>, MutableList<EntryRequirement>>()

    for (index in 0 until array.length()) {
        val item = array.getJSONObject(index)
        val requirement = parseEntryRequirement(item)
        require(ids.add(requirement.id)) {
            "entry_requirements.json: duplicate id ${requirement.id}"
        }
        grouped.getOrPut(requirement.passportIso to requirement.destinationIso) {
            mutableListOf()
        }.add(requirement)
    }

    return EntryRequirementDatabase(
        version = version,
        updated = updated,
        origin = origin,
        lastSuccessfulCheckAt = lastSuccessfulCheckAt,
        requirements = grouped.mapValues { (_, value) -> value.toList() }
    )
}

private fun parseEntryRequirement(item: JSONObject): EntryRequirement {
    val passportIso = item.optInt("passportIso", 0)
    val destinationIso = item.optInt("destinationIso", 0)
    require(passportIso in 1..999 && destinationIso in 1..999) {
        "entry_requirements.json: invalid passport/destination id"
    }
    require(passportIso != destinationIso) {
        "entry_requirements.json: home-country requirement is not allowed"
    }

    // This database is deliberately unable to redefine a visa category.
    // Any such fields mean the payload is malformed and must be rejected.
    listOf("status", "visaType", "visaStatus", "days", "stayDays", "mapCategory")
        .forEach { forbiddenField ->
            require(!item.has(forbiddenField)) {
                "entry_requirements.json: forbidden visa field $forbiddenField"
            }
        }

    require(item.has("mandatory") && item.get("mandatory") is Boolean) {
        "entry_requirements.json: invalid mandatory flag"
    }

    val sourceUrl = requiredEntryRequirementText(item, "sourceUrl", 2048)
    require(sourceUrl.startsWith("https://")) {
        "entry_requirements.json: only HTTPS sources are allowed"
    }
    URL(sourceUrl)

    return EntryRequirement(
        id = requiredEntryRequirementText(item, "id", 120),
        passportIso = passportIso,
        destinationIso = destinationIso,
        applicableVisaTypes = parseEntryRequirementVisaTypes(item.optJSONArray("visaTypes")),
        type = parseEntryRequirementType(requiredEntryRequirementText(item, "type", 80)),
        title = requiredEntryRequirementText(item, "title", 240),
        summary = requiredEntryRequirementText(item, "summary", 1200),
        timing = requiredEntryRequirementText(item, "timing", 500),
        mandatory = item.optBoolean("mandatory", false),
        steps = parseEntryRequirementStringArray(
            array = item.optJSONArray("steps"),
            field = "steps",
            minimum = 1,
            maximum = 12,
            maxTextLength = 600
        ),
        officialAuthority = requiredEntryRequirementText(item, "officialAuthority", 240),
        sourceUrl = sourceUrl,
        verified = requiredEntryRequirementText(item, "verified", 32)
    )
}

private fun parseEntryRequirementType(value: String): EntryRequirementType =
    when (value.trim().lowercase()) {
        "arrival_card" -> EntryRequirementType.ARRIVAL_CARD
        "pre_travel_registration" -> EntryRequirementType.PRE_TRAVEL_REGISTRATION
        "health_declaration" -> EntryRequirementType.HEALTH_DECLARATION
        "customs_declaration" -> EntryRequirementType.CUSTOMS_DECLARATION
        "tourism_registration" -> EntryRequirementType.TOURISM_REGISTRATION
        "other_entry_formality" -> EntryRequirementType.OTHER_ENTRY_FORMALITY
        else -> error("entry_requirements.json: unknown requirement type")
    }

private fun parseEntryRequirementVisaTypes(array: JSONArray?): Set<VisaType> {
    require(array != null && array.length() in 1..10) {
        "entry_requirements.json: invalid visaTypes"
    }

    return buildSet {
        for (index in 0 until array.length()) {
            add(
                when (array.getString(index).trim().lowercase()) {
                    "freedom" -> VisaType.FREEDOM
                    "visa_free" -> VisaType.VISA_FREE
                    "eta" -> VisaType.ETA
                    "visa_on_arrival" -> VisaType.VISA_ON_ARRIVAL
                    "e_visa" -> VisaType.E_VISA
                    "visa_required" -> VisaType.VISA_REQUIRED
                    "entry_restricted" -> VisaType.ENTRY_RESTRICTED
                    "special_permit" -> VisaType.SPECIAL_PERMIT
                    "mixed_requirements" -> VisaType.MIXED_REQUIREMENTS
                    "no_data" -> VisaType.NO_DATA
                    else -> error("entry_requirements.json: unknown visa type")
                }
            )
        }
    }
}

private fun parseEntryRequirementStringArray(
    array: JSONArray?,
    field: String,
    minimum: Int,
    maximum: Int,
    maxTextLength: Int
): List<String> {
    require(array != null && array.length() in minimum..maximum) {
        "entry_requirements.json: invalid $field"
    }
    return buildList {
        for (index in 0 until array.length()) {
            val value = array.getString(index).trim()
            require(value.isNotEmpty() && value.length <= maxTextLength) {
                "entry_requirements.json: invalid $field item"
            }
            add(value)
        }
    }
}

private fun requiredEntryRequirementText(
    json: JSONObject,
    field: String,
    maxLength: Int
): String {
    val value = json.optString(field).trim()
    require(value.isNotEmpty() && value.length <= maxLength) {
        "entry_requirements.json: invalid $field"
    }
    return value
}

internal fun saveEntryRequirementDatabaseAtomically(
    context: Context,
    jsonText: String
) {
    saveTextAtomically(
        context = context,
        targetFileName = EntryRequirementCacheFileName,
        previousFileName = EntryRequirementCachePreviousFileName,
        text = jsonText
    )
}

internal suspend fun checkForEntryRequirementDatabaseUpdate(
    context: Context,
    force: Boolean = false
): EntryRequirementUpdateResult = withContext(Dispatchers.IO) {
    val preferences =
        context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    val lastSuccessfulCheck =
        preferences.getLong(RemoteEntryRequirementLastCheckPreference, 0L)

    if (
        !force &&
        lastSuccessfulCheck > 0L &&
        now - lastSuccessfulCheck < EntryRequirementUpdateCheckIntervalMs
    ) {
        return@withContext EntryRequirementUpdateResult(
            status = VisaUpdateCheckStatus.SKIPPED,
            checkedAtMillis = lastSuccessfulCheck
        )
    }

    runCatching {
        val versionText = downloadUtf8Text(
            urlString = RemoteEntryRequirementVersionUrl,
            maxBytes = RemoteVersionMaxBytes
        )
        val manifest = parseRemoteDatabaseManifest(
            text = versionText,
            manifestUrl = RemoteEntryRequirementVersionUrl,
            releasePrefix = "entry_requirements",
            maxDatabaseBytes = RemoteEntryRequirementDatabaseMaxBytes
        )
        val expectedCount = manifest.json.optInt("requirementCount", -1)
        require(expectedCount in 0..5000) {
            "entry_requirements_version.json: invalid requirementCount"
        }
        val remoteVersion = manifest.version
        val currentVersion = loadEntryRequirementDatabase(context).version

        if (remoteVersion <= currentVersion) {
            preferences.edit()
                .putLong(RemoteEntryRequirementLastCheckPreference, now)
                .apply()

            return@runCatching EntryRequirementUpdateResult(
                status = VisaUpdateCheckStatus.CURRENT,
                remoteVersion = remoteVersion,
                checkedAtMillis = now
            )
        }

        val databaseText = downloadUtf8Text(
            urlString = manifest.databaseUrl,
            maxBytes = RemoteEntryRequirementDatabaseMaxBytes
        )
        verifyRemotePayload(databaseText, manifest)

        val database = parseEntryRequirementDatabase(
            text = databaseText,
            origin = VisaDatabaseOrigin.REMOTE,
            expectedVersion = remoteVersion,
            lastSuccessfulCheckAt = now
        )
        require(database.requirementCount() == expectedCount) {
            "entry_requirements.json: requirementCount mismatch"
        }

        saveEntryRequirementDatabaseAtomically(
            context = context,
            jsonText = databaseText
        )

        preferences.edit()
            .putInt(RemoteEntryRequirementVersionPreference, remoteVersion)
            .putLong(RemoteEntryRequirementLastCheckPreference, now)
            .apply()

        EntryRequirementUpdateResult(
            status = VisaUpdateCheckStatus.UPDATED,
            database = database,
            remoteVersion = remoteVersion,
            checkedAtMillis = now
        )
    }.getOrElse { error ->
        EntryRequirementUpdateResult(
            status = VisaUpdateCheckStatus.FAILED,
            failureMessage = when (error) {
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketTimeoutException ->
                    "Нет подключения к интернету"
                else ->
                    "Не удалось проверить дополнительные условия въезда"
            }
        )
    }
}
