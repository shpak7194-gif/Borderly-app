package com.example.borderly

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

internal const val RemoteEntryGuideVersionUrl =
    "https://shpak7194-gif.github.io/borderly-data/entry_guides_version.json"

internal const val EntryGuideCacheFileName = "entry_guides_remote.json"
internal const val EntryGuideCachePreviousFileName = "entry_guides_previous.json"
internal const val RemoteEntryGuideVersionPreference = "remote_entry_guides_version"
internal const val RemoteEntryGuideLastCheckPreference = "remote_entry_guides_last_check"
internal const val EntryGuideUpdateCheckIntervalMs = 24L * 60L * 60L * 1000L
internal const val RemoteEntryGuideDatabaseMaxBytes = 2 * 1024 * 1024
internal const val MinimumEntryGuideCount = 4

internal fun loadEntryGuideDatabase(context: Context): EntryGuideDatabase {
    val preferences =
        context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
    val lastSuccessfulCheckAt =
        preferences.getLong(RemoteEntryGuideLastCheckPreference, 0L)
    val cachedFile = File(context.filesDir, EntryGuideCacheFileName)
    val previousFile = File(context.filesDir, EntryGuideCachePreviousFileName)
    val bundled = parseEntryGuideDatabase(
        text = bundledEntryGuideDatabaseText(context),
        origin = VisaDatabaseOrigin.BUNDLED,
        expectedVersion = null,
        lastSuccessfulCheckAt = lastSuccessfulCheckAt
    )
    val candidates = mutableListOf(bundled)
    listOf(cachedFile, previousFile).forEach { file ->
        if (!file.isFile) return@forEach
        runCatching {
            candidates += parseEntryGuideDatabase(
                text = file.readText(Charsets.UTF_8),
                origin = VisaDatabaseOrigin.REMOTE,
                expectedVersion = null,
                lastSuccessfulCheckAt = lastSuccessfulCheckAt
            )
        }.onFailure { file.delete() }
    }
    return candidates.maxWithOrNull(
        compareBy<EntryGuideDatabase> { it.version }
            .thenBy { if (it.origin == VisaDatabaseOrigin.REMOTE) 1 else 0 }
    ) ?: bundled
}

internal fun bundledEntryGuideDatabaseText(context: Context): String =
    context.assets.open("entry_guides.json")
        .bufferedReader()
        .use { it.readText() }

internal fun parseEntryGuideDatabase(
    text: String,
    origin: VisaDatabaseOrigin,
    expectedVersion: Int?,
    lastSuccessfulCheckAt: Long
): EntryGuideDatabase {
    val root = JSONObject(text)
    require(root.optInt("schemaVersion", 0) == 1) {
        "entry_guides.json: unsupported schemaVersion"
    }

    val version = root.optInt("version", 0)
    require(version > 0) {
        "entry_guides.json: invalid version"
    }
    if (expectedVersion != null) {
        require(version == expectedVersion) {
            "entry_guides.json: version mismatch"
        }
    }

    val updated = requiredGuideText(root, "updated", 32)
    val array = root.optJSONArray("guides")
        ?: error("entry_guides.json: missing guides array")

    require(array.length() in MinimumEntryGuideCount..1000) {
        "entry_guides.json: invalid guide count"
    }

    val guides = LinkedHashMap<Pair<Int, Int>, PassportEntryGuide>()

    for (index in 0 until array.length()) {
        val item = array.getJSONObject(index)
        val guide = parseEntryGuide(item)
        val key = guide.passportIso to guide.destinationIso
        require(key !in guides) {
            "entry_guides.json: duplicate ${guide.passportIso}->${guide.destinationIso}"
        }
        guides[key] = guide
    }

    return EntryGuideDatabase(
        version = version,
        updated = updated,
        origin = origin,
        lastSuccessfulCheckAt = lastSuccessfulCheckAt,
        guides = guides
    )
}

private fun parseEntryGuide(item: JSONObject): PassportEntryGuide {
    val passportIso = item.optInt("passportIso", 0)
    val destinationIso = item.optInt("destinationIso", 0)
    require(passportIso in 1..999 && destinationIso in 1..999) {
        "entry_guides.json: invalid passport/destination id"
    }
    require(passportIso != destinationIso) {
        "entry_guides.json: home-country guide is not allowed"
    }

    val visaTypes = parseVisaTypes(item.optJSONArray("visaTypes"))
    val steps = parseGuideStringArray(
        array = item.optJSONArray("steps"),
        field = "steps",
        minimum = 1,
        maximum = 12,
        maxTextLength = 500
    )
    val documents = parseGuideStringArray(
        array = item.optJSONArray("documents"),
        field = "documents",
        minimum = 1,
        maximum = 24,
        maxTextLength = 500
    )

    val linksArray = item.optJSONArray("links")
        ?: error("entry_guides.json: missing links")
    require(linksArray.length() in 1..6) {
        "entry_guides.json: invalid link count"
    }

    val links = buildList {
        var primaryCount = 0
        for (index in 0 until linksArray.length()) {
            val linkJson = linksArray.getJSONObject(index)
            val url = requiredGuideText(linkJson, "url", 2048)
            require(url.startsWith("https://")) {
                "entry_guides.json: only HTTPS links are allowed"
            }
            // Parse once so malformed URLs cannot enter the working database.
            URL(url)

            val primary = linkJson.optBoolean("primary", false)
            if (primary) primaryCount += 1

            add(
                OfficialEntryLink(
                    title = requiredGuideText(linkJson, "title", 160),
                    url = url,
                    primary = primary
                )
            )
        }
        require(primaryCount == 1) {
            "entry_guides.json: every guide must have exactly one primary link"
        }
    }

    return PassportEntryGuide(
        passportIso = passportIso,
        destinationIso = destinationIso,
        applicableVisaTypes = visaTypes,
        permitName = requiredGuideText(item, "permitName", 240),
        applicationMethod = requiredGuideText(item, "applicationMethod", 240),
        steps = steps,
        documents = documents,
        documentsNote = requiredGuideText(item, "documentsNote", 800),
        fee = optionalGuideText(item, "fee", 240),
        timing = optionalGuideText(item, "timing", 400),
        extraNote = optionalGuideText(item, "extraNote", 800),
        officialAuthority = requiredGuideText(item, "officialAuthority", 240),
        verified = requiredGuideText(item, "verified", 32),
        links = links
    )
}

private fun parseVisaTypes(array: JSONArray?): Set<VisaType> {
    require(array != null && array.length() in 1..10) {
        "entry_guides.json: invalid visaTypes"
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
                    else -> error("entry_guides.json: unknown visa type")
                }
            )
        }
    }
}

private fun parseGuideStringArray(
    array: JSONArray?,
    field: String,
    minimum: Int,
    maximum: Int,
    maxTextLength: Int
): List<String> {
    require(array != null && array.length() in minimum..maximum) {
        "entry_guides.json: invalid $field"
    }
    return buildList {
        for (index in 0 until array.length()) {
            val value = array.getString(index).trim()
            require(value.isNotEmpty() && value.length <= maxTextLength) {
                "entry_guides.json: invalid $field item"
            }
            add(value)
        }
    }
}

private fun requiredGuideText(
    json: JSONObject,
    field: String,
    maxLength: Int
): String {
    val value = json.optString(field).trim()
    require(value.isNotEmpty() && value.length <= maxLength) {
        "entry_guides.json: invalid $field"
    }
    return value
}

private fun optionalGuideText(
    json: JSONObject,
    field: String,
    maxLength: Int
): String? {
    val value = json.optString(field).trim()
    if (value.isEmpty()) return null
    require(value.length <= maxLength) {
        "entry_guides.json: invalid $field"
    }
    return value
}

internal fun saveEntryGuideDatabaseAtomically(
    context: Context,
    jsonText: String
) {
    saveTextAtomically(
        context = context,
        targetFileName = EntryGuideCacheFileName,
        previousFileName = EntryGuideCachePreviousFileName,
        text = jsonText
    )
}

internal suspend fun checkForEntryGuideDatabaseUpdate(
    context: Context,
    force: Boolean = false
): EntryGuideUpdateResult = withContext(Dispatchers.IO) {
    val preferences =
        context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    val lastSuccessfulCheck =
        preferences.getLong(RemoteEntryGuideLastCheckPreference, 0L)

    if (
        !force &&
        lastSuccessfulCheck > 0L &&
        now - lastSuccessfulCheck < EntryGuideUpdateCheckIntervalMs
    ) {
        return@withContext EntryGuideUpdateResult(
            status = VisaUpdateCheckStatus.SKIPPED,
            checkedAtMillis = lastSuccessfulCheck
        )
    }

    runCatching {
        val versionText = downloadUtf8Text(
            urlString = RemoteEntryGuideVersionUrl,
            maxBytes = RemoteVersionMaxBytes
        )
        val manifest = parseRemoteDatabaseManifest(
            text = versionText,
            manifestUrl = RemoteEntryGuideVersionUrl,
            releasePrefix = "entry_guides",
            maxDatabaseBytes = RemoteEntryGuideDatabaseMaxBytes
        )
        val expectedCount = manifest.json.optInt("guideCount", -1)
        require(expectedCount in MinimumEntryGuideCount..1000) {
            "entry_guides_version.json: invalid guideCount"
        }
        val remoteVersion = manifest.version
        val currentVersion = loadEntryGuideDatabase(context).version

        if (remoteVersion <= currentVersion) {
            preferences.edit()
                .putLong(RemoteEntryGuideLastCheckPreference, now)
                .apply()

            return@runCatching EntryGuideUpdateResult(
                status = VisaUpdateCheckStatus.CURRENT,
                remoteVersion = remoteVersion,
                checkedAtMillis = now
            )
        }

        val databaseText = downloadUtf8Text(
            urlString = manifest.databaseUrl,
            maxBytes = RemoteEntryGuideDatabaseMaxBytes
        )
        verifyRemotePayload(databaseText, manifest)

        val database = parseEntryGuideDatabase(
            text = databaseText,
            origin = VisaDatabaseOrigin.REMOTE,
            expectedVersion = remoteVersion,
            lastSuccessfulCheckAt = now
        )
        require(database.guideCount() == expectedCount) {
            "entry_guides.json: guideCount mismatch"
        }

        saveEntryGuideDatabaseAtomically(
            context = context,
            jsonText = databaseText
        )

        preferences.edit()
            .putInt(RemoteEntryGuideVersionPreference, remoteVersion)
            .putLong(RemoteEntryGuideLastCheckPreference, now)
            .apply()

        EntryGuideUpdateResult(
            status = VisaUpdateCheckStatus.UPDATED,
            database = database,
            remoteVersion = remoteVersion,
            checkedAtMillis = now
        )
    }.getOrElse { error ->
        EntryGuideUpdateResult(
            status = VisaUpdateCheckStatus.FAILED,
            failureMessage = when (error) {
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketTimeoutException ->
                    "Нет подключения к интернету"
                else ->
                    "Не удалось проверить обновление гайдов"
            }
        )
    }
}
