package com.example.borderly

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

internal data class RemoteDatabaseManifest(
    val version: Int,
    val databaseUrl: String,
    val databaseSha256: String,
    val databaseBytes: Int,
    val json: JSONObject
)

internal fun parseRemoteDatabaseManifest(
    text: String,
    manifestUrl: String,
    releasePrefix: String,
    maxDatabaseBytes: Int
): RemoteDatabaseManifest {
    val json = JSONObject(text)
    require(json.optInt("schemaVersion", 0) == 1) {
        "Unsupported remote release schema"
    }
    val version = json.optInt("version", 0)
    require(version > 0) { "Invalid remote version" }

    val expectedLocation = "releases/${releasePrefix}_v$version.json"
    val databaseLocation = json.optString("database")
    require(databaseLocation == expectedLocation) {
        "Remote database must use an immutable versioned filename"
    }

    val manifest = URL(manifestUrl)
    val database = URL(manifest, databaseLocation)
    require(database.protocol.equals("https", ignoreCase = true)) {
        "Only HTTPS is allowed for remote data"
    }
    require(
        database.host.equals(manifest.host, ignoreCase = true) &&
            database.port == manifest.port
    ) {
        "Remote manifest cannot redirect data to another origin"
    }

    val hash = json.optString("databaseSha256").lowercase()
    require(hash.matches(Regex("[a-f0-9]{64}"))) {
        "Invalid remote database SHA-256"
    }
    val bytes = json.optInt("databaseBytes", 0)
    require(bytes in 2..maxDatabaseBytes) {
        "Invalid remote database size"
    }

    return RemoteDatabaseManifest(
        version = version,
        databaseUrl = database.toString(),
        databaseSha256 = hash,
        databaseBytes = bytes,
        json = json
    )
}

internal fun verifyRemotePayload(
    text: String,
    manifest: RemoteDatabaseManifest
) {
    val bytes = text.toByteArray(Charsets.UTF_8)
    require(bytes.size == manifest.databaseBytes) {
        "Remote database byte count mismatch"
    }
    val actualHash = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    require(actualHash == manifest.databaseSha256) {
        "Remote database SHA-256 mismatch"
    }
}

internal fun saveTextAtomically(
    context: Context,
    targetFileName: String,
    previousFileName: String,
    text: String
) {
    val target = File(context.filesDir, targetFileName)
    val previous = File(context.filesDir, previousFileName)
    if (target.isFile) {
        target.copyTo(previous, overwrite = true)
    }

    val atomicFile = AtomicFile(target)
    var output: FileOutputStream? = null
    try {
        output = atomicFile.startWrite()
        output.write(text.toByteArray(Charsets.UTF_8))
        output.flush()
        output.fd.sync()
        atomicFile.finishWrite(output)
    } catch (error: Throwable) {
        output?.let(atomicFile::failWrite)
        throw error
    }
}
