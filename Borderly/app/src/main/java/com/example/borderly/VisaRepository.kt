package com.example.borderly

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL

internal const val RemoteVisaVersionUrl = "https://shpak7194-gif.github.io/borderly-data/version.json"

internal const val VisaCacheFileName = "visa_requirements_remote.json"
internal const val VisaCachePreviousFileName = "visa_requirements_previous.json"
internal const val RemoteVisaVersionPreference = "remote_visa_version"
internal const val RemoteVisaLastCheckPreference = "remote_visa_last_check"
internal const val VisaUpdateCheckIntervalMs = 24L * 60L * 60L * 1000L
internal const val RemoteVersionMaxBytes = 128 * 1024
internal const val RemoteVisaDatabaseMaxBytes = 12 * 1024 * 1024
internal const val RemoteVisaConnectTimeoutMs = 15_000
internal const val RemoteVisaReadTimeoutMs = 30_000
internal const val SupportedVisaSchemaVersion = 1
internal const val SupportedVisaTaxonomyVersion = 2
internal const val SupportedVisaProvenanceVersion = 1
internal const val ExpectedPassportCount = 199
internal const val ExpectedDestinationCount = 248
internal const val ExpectedRulesPerPassport = ExpectedDestinationCount - 1

private val AllowedRemoteVisaStatuses = setOf(
    "freedom",
    "visa free",
    "eta",
    "visa on arrival",
    "e-visa",
    "visa required",
    "entry restricted",
    "special permit",
    "mixed requirements",
    "no data"
)

private const val VisaUpdateLogTag = "BorderlyVisaUpdate"
private val VisaDownloadRetryDelaysMs = longArrayOf(0L, 1_500L, 4_000L)
private val RetryableHttpStatuses = setOf(408, 425, 429, 500, 502, 503, 504)

internal class RemoteHttpException(
    val statusCode: Int
) : IOException("HTTP $statusCode")

internal fun isRetryableVisaDownloadError(error: Throwable): Boolean = when (error) {
    is UnknownHostException,
    is ConnectException,
    is SocketTimeoutException -> true
    is RemoteHttpException -> error.statusCode in RetryableHttpStatuses
    else -> false
}

internal fun parseVisaDatabase(
    text: String,
    origin: VisaDatabaseOrigin,
    version: Int,
    lastSuccessfulCheckAt: Long = 0L
): VisaDatabase {
    val root = JSONObject(text)
    require(root.optInt("schemaVersion", 0) == SupportedVisaSchemaVersion) {
        "visa_requirements.json: unsupported schemaVersion"
    }
    val embeddedVersion = root.optInt("dataVersion", 0)
    require(embeddedVersion > 0) {
        "visa_requirements.json: missing dataVersion"
    }
    if (version > 0) {
        require(embeddedVersion == version) {
            "visa_requirements.json: dataVersion mismatch"
        }
    }
    val passportsJson = root.optJSONObject("passports")
        ?: error("visa_requirements.json: missing passports object")
    val destinationCount = root.optInt("destinationCount", 0)

    validateVisaMatrix(
        passportsJson = passportsJson,
        destinationCount = destinationCount
    )
    val databaseSource = root.optString("source", "Borderly Visa Data")
    val databaseSourceUrl = root.optString("sourceUrl").takeIf { it.isNotBlank() }
    val databaseUpdated = root.optString("updated").trim()
    require(databaseUpdated.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
        "visa_requirements.json: invalid updated date"
    }
    val sources = parseVisaSources(root, embeddedVersion)
    val destinationSourceIds = parseDestinationSourceIds(
        root = root,
        passportsJson = passportsJson,
        sources = sources,
        embeddedVersion = embeddedVersion
    )
    val requirementsByPassport = parseVisaRequirements(
        passportsJson = passportsJson,
        sources = sources,
        destinationSourceIds = destinationSourceIds,
        databaseSource = databaseSource,
        databaseSourceUrl = databaseSourceUrl,
        databaseUpdated = databaseUpdated,
        embeddedVersion = embeddedVersion
    )

    val database = VisaDatabase(
        source = databaseSource,
        sourceUrl = databaseSourceUrl.orEmpty(),
        updated = databaseUpdated,
        origin = origin,
        version = embeddedVersion,
        destinationCount = destinationCount,
        lastSuccessfulCheckAt = lastSuccessfulCheckAt,
        requirementsByPassport = requirementsByPassport
    )

    return database
}

private fun parseVisaSources(
    root: JSONObject,
    embeddedVersion: Int
): Map<String, VisaSource> {
    val sourcesJson = root.optJSONArray("sources")
    if (sourcesJson == null) {
        require(embeddedVersion < 13) {
            "visa_requirements.json: v13 requires sources registry"
        }
        return emptyMap()
    }

    return buildMap {
        for (index in 0 until sourcesJson.length()) {
            val item = sourcesJson.optJSONObject(index)
                ?: error("visa_requirements.json: malformed source at $index")
            val id = item.optString("id").trim()
            val name = item.optString("name").trim()
            val url = item.optString("url").trim()
            val snapshotUrl = item.optString("snapshotUrl")
                .trim()
                .takeIf { it.isNotBlank() }
            val type = VisaSourceType.fromStorage(item.optString("type"))
            val description = item.optString("description").takeIf { it.isNotBlank() }
            require(id.isNotBlank() && name.isNotBlank()) {
                "visa_requirements.json: source id and name are required"
            }
            require(url.startsWith("https://")) {
                "visa_requirements.json: source $id must use HTTPS"
            }
            require(snapshotUrl == null || snapshotUrl.startsWith("https://")) {
                "visa_requirements.json: source $id snapshot must use HTTPS"
            }
            require(type == VisaSourceType.DATASET || type == VisaSourceType.DERIVED) {
                "visa_requirements.json: registry source $id has unsupported type"
            }
            if (embeddedVersion >= 13) {
                require(description != null) {
                    "visa_requirements.json: source $id requires a description"
                }
            }
            require(id !in this) {
                "visa_requirements.json: duplicate source id $id"
            }
            put(
                id,
                VisaSource(
                    id = id,
                    name = name,
                    url = snapshotUrl ?: url,
                    type = type,
                    description = description,
                    license = item.optString("license").takeIf { it.isNotBlank() }
                )
            )
        }
    }.also { sources ->
        if (embeddedVersion >= 13) {
            require(sources.isNotEmpty()) {
                "visa_requirements.json: v13 sources registry is empty"
            }
        }
    }
}

private fun parseDestinationSourceIds(
    root: JSONObject,
    passportsJson: JSONObject,
    sources: Map<String, VisaSource>,
    embeddedVersion: Int
): Map<Int, String> {
    val provenance = root.optJSONObject("provenance")
    if (provenance == null) {
        require(embeddedVersion < 13) {
            "visa_requirements.json: v13 requires provenance"
        }
        return emptyMap()
    }
    require(provenance.optInt("schemaVersion", 0) == SupportedVisaProvenanceVersion) {
        "visa_requirements.json: unsupported provenance schemaVersion"
    }
    val mappingJson = provenance.optJSONObject("destinationSourceIds")
        ?: error("visa_requirements.json: missing destinationSourceIds")
    val mapping = buildMap {
        val keys = mappingJson.keys()
        while (keys.hasNext()) {
            val rawDestinationId = keys.next()
            val destinationId = rawDestinationId.toIntOrNull()
                ?: error("visa_requirements.json: invalid provenance destination $rawDestinationId")
            val sourceId = mappingJson.optString(rawDestinationId).trim()
            require(sourceId in sources) {
                "visa_requirements.json: unknown provenance source $sourceId"
            }
            put(destinationId, sourceId)
        }
    }

    val destinationIds = buildSet {
        val passportKeys = passportsJson.keys()
        while (passportKeys.hasNext()) {
            val passportId = passportKeys.next()
            add(passportId.toInt())
            val row = passportsJson.getJSONObject(passportId)
            val destinationKeys = row.keys()
            while (destinationKeys.hasNext()) add(destinationKeys.next().toInt())
        }
    }
    require(mapping.keys == destinationIds) {
        "visa_requirements.json: provenance must cover all destinations"
    }
    return mapping
}

private fun parseVisaRequirements(
    passportsJson: JSONObject,
    sources: Map<String, VisaSource>,
    destinationSourceIds: Map<Int, String>,
    databaseSource: String,
    databaseSourceUrl: String?,
    databaseUpdated: String,
    embeddedVersion: Int
): Map<Int, Map<Int, VisaRequirement>> = buildMap {
    val passports = passportsJson.keys()
    while (passports.hasNext()) {
        val passportId = passports.next().toInt()
        val passportJson = passportsJson.getJSONObject(passportId.toString())
        put(
            passportId,
            buildMap {
                val destinations = passportJson.keys()
                while (destinations.hasNext()) {
                    val destinationId = destinations.next().toInt()
                    val rule = passportJson.getJSONObject(destinationId.toString())
                    val status = rule.getString("status")
                    val explicitSource = rule.optString("source").takeIf { it.isNotBlank() }
                    val explicitSourceUrl = rule.optString("sourceUrl").takeIf { it.isNotBlank() }
                    require((explicitSource == null) == (explicitSourceUrl == null)) {
                        "visa_requirements.json: incomplete rule source $passportId->$destinationId"
                    }
                    val registrySource = destinationSourceIds[destinationId]?.let(sources::get)
                    val sourceIsRuleSpecific = explicitSource != null && explicitSourceUrl != null
                    val sourceType = if (sourceIsRuleSpecific) {
                        VisaSourceType.fromStorage(rule.optString("sourceType"))
                            .takeUnless { it == VisaSourceType.UNKNOWN }
                            ?: VisaSourceType.OFFICIAL
                    } else {
                        registrySource?.type ?: VisaSourceType.DATASET
                    }
                    if (embeddedVersion >= 13 && sourceIsRuleSpecific) {
                        require(
                            sourceType == VisaSourceType.OFFICIAL ||
                                sourceType == VisaSourceType.CORROBORATED
                        ) {
                            "visa_requirements.json: invalid rule source type $passportId->$destinationId"
                        }
                    }
                    put(
                        destinationId,
                        VisaRequirement(
                            status = status,
                            visaType = visaTypeFromStorageStatus(status),
                            stayDays = if (rule.has("days")) {
                                rule.getInt("days")
                            } else {
                                null
                            },
                            source = explicitSource ?: registrySource?.name ?: databaseSource,
                            sourceUrl = explicitSourceUrl ?: registrySource?.url ?: databaseSourceUrl,
                            updated = rule.optString("updated").takeIf { it.isNotBlank() }
                                ?: databaseUpdated,
                            validUntil = rule.optString("validUntil").takeIf { it.isNotBlank() },
                            note = rule.optString("note").takeIf { it.isNotBlank() },
                            sourceType = sourceType,
                            sourceDescription = if (sourceIsRuleSpecific) {
                                rule.optString("sourceDescription").takeIf { it.isNotBlank() }
                            } else {
                                registrySource?.description
                            },
                            sourceLicense = if (sourceIsRuleSpecific) {
                                rule.optString("sourceLicense").takeIf { it.isNotBlank() }
                            } else {
                                registrySource?.license
                            },
                            sourceIsRuleSpecific = sourceIsRuleSpecific
                        )
                    )
                }
            }
        )
    }
}

private fun validateVisaMatrix(
    passportsJson: JSONObject,
    destinationCount: Int
) {
    require(destinationCount == ExpectedDestinationCount) {
        "visa_requirements.json: expected $ExpectedDestinationCount destinations"
    }

    val passportIds = buildSet {
        val keys = passportsJson.keys()
        while (keys.hasNext()) {
            val raw = keys.next()
            val id = raw.toIntOrNull()
                ?: error("visa_requirements.json: invalid passport id $raw")
            require(id in 1..999) { "visa_requirements.json: invalid passport id $id" }
            add(id)
        }
    }
    require(passportIds.size == ExpectedPassportCount) {
        "visa_requirements.json: expected $ExpectedPassportCount passports"
    }

    val destinationIds = passportIds.toMutableSet()
    for (passportId in passportIds) {
        val row = passportsJson.optJSONObject(passportId.toString())
            ?: error("visa_requirements.json: missing row $passportId")
        require(row.length() == ExpectedRulesPerPassport) {
            "visa_requirements.json: passport $passportId must have $ExpectedRulesPerPassport rules"
        }
        val keys = row.keys()
        while (keys.hasNext()) {
            val rawDestinationId = keys.next()
            val destinationId = rawDestinationId.toIntOrNull()
                ?: error("visa_requirements.json: invalid destination id $rawDestinationId")
            require(destinationId in 1..999 && destinationId != passportId) {
                "visa_requirements.json: invalid pair $passportId->$destinationId"
            }
            destinationIds += destinationId

            val rule = row.optJSONObject(rawDestinationId)
                ?: error("visa_requirements.json: malformed rule $passportId->$destinationId")
            val status = rule.optString("status")
            require(status in AllowedRemoteVisaStatuses) {
                "visa_requirements.json: unknown status $status at $passportId->$destinationId"
            }
            if (rule.has("days")) {
                val value = rule.opt("days")
                require(value is Number) {
                    "visa_requirements.json: non-numeric days at $passportId->$destinationId"
                }
                val days = value.toInt()
                require(days in 1..3660 && value.toDouble() == days.toDouble()) {
                    "visa_requirements.json: invalid days at $passportId->$destinationId"
                }
            }
            rule.optString("sourceUrl")
                .takeIf { it.isNotBlank() }
                ?.let { sourceUrl ->
                    require(sourceUrl.startsWith("https://")) {
                        "visa_requirements.json: non-HTTPS source at $passportId->$destinationId"
                    }
                }
        }
    }

    require(destinationIds.size == ExpectedDestinationCount) {
        "visa_requirements.json: destination universe is incomplete"
    }
    for (passportId in passportIds) {
        val row = passportsJson.getJSONObject(passportId.toString())
        val actual = buildSet {
            val keys = row.keys()
            while (keys.hasNext()) add(keys.next().toInt())
        }
        val expected = destinationIds - passportId
        require(actual == expected) {
            "visa_requirements.json: row $passportId has missing or unexpected destinations"
        }
    }
}

internal fun bundledVisaDatabaseText(context: Context): String =
    context.assets.open("visa_requirements.json")
        .bufferedReader()
        .use { it.readText() }

internal fun loadVisaDatabase(context: Context): VisaDatabase {
    val cachedFile = File(context.filesDir, VisaCacheFileName)
    val previousFile = File(context.filesDir, VisaCachePreviousFileName)
    val preferences =
        context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)
    val lastSuccessfulCheckAt =
        preferences.getLong(RemoteVisaLastCheckPreference, 0L)

    val bundled = parseVisaDatabase(
        text = bundledVisaDatabaseText(context),
        origin = VisaDatabaseOrigin.BUNDLED,
        version = 0,
        lastSuccessfulCheckAt = lastSuccessfulCheckAt
    )
    val candidates = mutableListOf(bundled)

    listOf(cachedFile, previousFile).forEach { file ->
        if (!file.isFile) return@forEach
        runCatching {
            candidates += parseVisaDatabase(
                text = file.readText(Charsets.UTF_8),
                origin = VisaDatabaseOrigin.REMOTE,
                version = 0,
                lastSuccessfulCheckAt = lastSuccessfulCheckAt
            )
        }.onFailure {
            // A broken cache must never break the application or replace a
            // newer database bundled with an app update.
            file.delete()
        }
    }

    return candidates.maxWithOrNull(
        compareBy<VisaDatabase> { it.version }
            .thenBy { if (it.origin == VisaDatabaseOrigin.REMOTE) 1 else 0 }
    ) ?: bundled
}

internal fun downloadUtf8Text(
    urlString: String,
    maxBytes: Int
): String {
    val url = URL(urlString)
    require(url.protocol.equals("https", ignoreCase = true)) {
        "Only HTTPS is allowed for visa updates"
    }

    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = RemoteVisaConnectTimeoutMs
        readTimeout = RemoteVisaReadTimeoutMs
        useCaches = false
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Borderly-Android")
        setRequestProperty("Cache-Control", "no-cache, no-store")
        setRequestProperty("Pragma", "no-cache")
    }

    try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw RemoteHttpException(responseCode)
        }

        val declaredLength = connection.contentLengthLong
        require(declaredLength <= 0L || declaredLength <= maxBytes.toLong()) {
            "Remote file is too large"
        }

        val output = ByteArrayOutputStream(
            if (declaredLength in 1..maxBytes.toLong()) {
                declaredLength.toInt()
            } else {
                32 * 1024
            }
        )

        connection.inputStream.use { input ->
            val buffer = ByteArray(8 * 1024)
            var total = 0

            while (true) {
                val count = input.read(buffer)
                if (count < 0) break

                total += count
                require(total <= maxBytes) {
                    "Remote file exceeded size limit"
                }
                output.write(buffer, 0, count)
            }
        }

        return output.toString(Charsets.UTF_8.name())
    } finally {
        connection.disconnect()
    }
}

internal suspend fun downloadVisaTextWithRetry(
    urlString: String,
    maxBytes: Int
): String {
    var lastError: Throwable? = null

    for (attempt in VisaDownloadRetryDelaysMs.indices) {
        if (attempt > 0) delay(VisaDownloadRetryDelaysMs[attempt])

        try {
            return downloadUtf8Text(
                urlString = urlString,
                maxBytes = maxBytes
            )
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            lastError = error
            if (!isRetryableVisaDownloadError(error)) throw error
            Log.w(
                VisaUpdateLogTag,
                "Visa data download attempt ${attempt + 1} of ${VisaDownloadRetryDelaysMs.size} failed",
                error
            )
        }
    }

    throw lastError ?: IOException("Visa data download failed")
}

internal fun saveVisaDatabaseAtomically(
    context: Context,
    jsonText: String
) {
    saveTextAtomically(
        context = context,
        targetFileName = VisaCacheFileName,
        previousFileName = VisaCachePreviousFileName,
        text = jsonText
    )
}

internal suspend fun checkForVisaDatabaseUpdate(
    context: Context,
    force: Boolean = false
): VisaUpdateCheckResult = withContext(Dispatchers.IO) {
    if (RemoteVisaVersionUrl.isBlank()) {
        return@withContext VisaUpdateCheckResult(
            status = VisaUpdateCheckStatus.FAILED
        )
    }

    val preferences =
        context.getSharedPreferences(BorderlyPreferences, Context.MODE_PRIVATE)

    val now = System.currentTimeMillis()
    val lastSuccessfulCheck =
        preferences.getLong(RemoteVisaLastCheckPreference, 0L)

    if (
        !force &&
        lastSuccessfulCheck > 0L &&
        now - lastSuccessfulCheck < VisaUpdateCheckIntervalMs
    ) {
        return@withContext VisaUpdateCheckResult(
            status = VisaUpdateCheckStatus.SKIPPED,
            checkedAtMillis = lastSuccessfulCheck
        )
    }

    runCatching {
        val versionText = downloadVisaTextWithRetry(
            urlString = RemoteVisaVersionUrl,
            maxBytes = RemoteVersionMaxBytes
        )
        val manifest = parseRemoteDatabaseManifest(
            text = versionText,
            manifestUrl = RemoteVisaVersionUrl,
            releasePrefix = "visa_requirements",
            maxDatabaseBytes = RemoteVisaDatabaseMaxBytes
        )
        require(manifest.json.optInt("taxonomyVersion", 0) == SupportedVisaTaxonomyVersion) {
            "version.json: unsupported taxonomyVersion"
        }
        if (manifest.version >= 13) {
            require(
                manifest.json.optInt("provenanceVersion", 0) ==
                    SupportedVisaProvenanceVersion
            ) {
                "version.json: unsupported provenanceVersion"
            }
        }
        require(
            manifest.json.optInt("passportCount", 0) == ExpectedPassportCount &&
                manifest.json.optInt("destinationCount", 0) == ExpectedDestinationCount &&
                manifest.json.optInt("rulesPerPassport", 0) == ExpectedRulesPerPassport
        ) {
            "version.json: matrix contract mismatch"
        }

        val remoteVersion = manifest.version
        val currentVersion = loadVisaDatabase(context).version

        if (remoteVersion <= currentVersion) {
            preferences.edit()
                .putLong(RemoteVisaLastCheckPreference, now)
                .apply()

            return@runCatching VisaUpdateCheckResult(
                status = VisaUpdateCheckStatus.CURRENT,
                remoteVersion = remoteVersion,
                checkedAtMillis = now
            )
        }

        val databaseText = downloadVisaTextWithRetry(
            urlString = manifest.databaseUrl,
            maxBytes = RemoteVisaDatabaseMaxBytes
        )
        verifyRemotePayload(databaseText, manifest)

        // Parse and validate completely BEFORE replacing the working cache.
        val database = parseVisaDatabase(
            text = databaseText,
            origin = VisaDatabaseOrigin.REMOTE,
            version = remoteVersion,
            lastSuccessfulCheckAt = now
        )

        saveVisaDatabaseAtomically(
            context = context,
            jsonText = databaseText
        )

        preferences.edit()
            .putInt(RemoteVisaVersionPreference, remoteVersion)
            .putLong(RemoteVisaLastCheckPreference, now)
            .apply()

        VisaUpdateCheckResult(
            status = VisaUpdateCheckStatus.UPDATED,
            database = database,
            remoteVersion = remoteVersion,
            checkedAtMillis = now
        )
    }.getOrElse { error ->
        if (error is CancellationException) throw error
        Log.w(
            VisaUpdateLogTag,
            "Remote visa update failed; the last verified database remains active",
            error
        )
        VisaUpdateCheckResult(
            status = VisaUpdateCheckStatus.FAILED,
            failureMessage = when (error) {
                is UnknownHostException,
                is ConnectException,
                is SocketTimeoutException ->
                    "Нет подключения к интернету"
                else ->
                    "Не удалось проверить обновления"
            }
        )
    }
}

