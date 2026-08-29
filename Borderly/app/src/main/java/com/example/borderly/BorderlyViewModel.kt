package com.example.borderly

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface BorderlyDataState {
    data object Loading : BorderlyDataState

    data class Ready(
        val nativeMap: NativeMapData?,
        val visaDatabase: VisaDatabase,
        val entryGuideDatabase: EntryGuideDatabase,
        val entryRequirementDatabase: EntryRequirementDatabase
    ) : BorderlyDataState

    data class Error(
        val message: String
    ) : BorderlyDataState
}

/**
 * Owns application data that used to be synchronously read from Compose.
 *
 * Native map parsing and visa database disk I/O now run on Dispatchers.IO.
 * Compose only observes the resulting state.
 */
internal class BorderlyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    var dataState: BorderlyDataState by mutableStateOf(BorderlyDataState.Loading)
        private set

    init {
        loadInitialData()
    }

    internal fun retry() {
        if (dataState == BorderlyDataState.Loading) return
        loadInitialData()
    }

    private fun applyDatabase(database: VisaDatabase) {
        val current = dataState
        if (current is BorderlyDataState.Ready) {
            dataState = current.copy(visaDatabase = database)
        }
    }

    private fun applySuccessfulCheckTimestamp(timestamp: Long) {
        val current = dataState
        if (current is BorderlyDataState.Ready) {
            dataState = current.copy(
                visaDatabase = current.visaDatabase
                    .withLastSuccessfulCheckAt(timestamp)
            )
        }
    }

    private fun applyEntryGuideDatabase(database: EntryGuideDatabase) {
        val current = dataState
        if (current is BorderlyDataState.Ready) {
            dataState = current.copy(entryGuideDatabase = database)
        }
    }

    private fun applyEntryGuideSuccessfulCheckTimestamp(timestamp: Long) {
        val current = dataState
        if (current is BorderlyDataState.Ready) {
            dataState = current.copy(
                entryGuideDatabase = current.entryGuideDatabase
                    .withLastSuccessfulCheckAt(timestamp)
            )
        }
    }

    private fun applyEntryRequirementDatabase(database: EntryRequirementDatabase) {
        val current = dataState
        if (current is BorderlyDataState.Ready) {
            dataState = current.copy(entryRequirementDatabase = database)
        }
    }

    private fun applyEntryRequirementSuccessfulCheckTimestamp(timestamp: Long) {
        val current = dataState
        if (current is BorderlyDataState.Ready) {
            dataState = current.copy(
                entryRequirementDatabase = current.entryRequirementDatabase
                    .withLastSuccessfulCheckAt(timestamp)
            )
        }
    }

    private fun loadInitialData() {
        dataState = BorderlyDataState.Loading

        scope.launch {
            val context = getApplication<Application>().applicationContext

            val initialResult = withContext(Dispatchers.IO) {
                runCatching {
                    // Database is required for the app to be useful.
                    val database = loadVisaDatabase(context)

                    // Entry guides have an independent bundled/remote fallback.
                    // If the guide asset is ever broken, the visa app still starts.
                    val entryGuideDatabase = runCatching {
                        loadEntryGuideDatabase(context)
                    }.getOrElse {
                        emptyEntryGuideDatabase()
                    }

                    // Non-visa entry formalities have their own independent fallback.
                    // A broken/stale formality payload must never block visa data.
                    val entryRequirementDatabase = runCatching {
                        loadEntryRequirementDatabase(context)
                    }.getOrElse {
                        emptyEntryRequirementDatabase()
                    }

                    // The native vector map is optional because the existing UI
                    // already has a static-map fallback when it cannot be parsed.
                    val nativeMap = runCatching {
                        loadNativeWorldMap(context)
                    }.getOrNull()

                    BorderlyDataState.Ready(
                        nativeMap = nativeMap,
                        visaDatabase = database,
                        entryGuideDatabase = entryGuideDatabase,
                        entryRequirementDatabase = entryRequirementDatabase
                    )
                }
            }

            initialResult.fold(
                onSuccess = { readyState ->
                    dataState = readyState
                    checkForRemoteUpdate()
                },
                onFailure = { error ->
                    dataState = BorderlyDataState.Error(
                        message = error.message
                            ?: context.getString(R.string.local_database_error)
                    )
                }
            )
        }
    }

    private fun checkForRemoteUpdate() {
        scope.launch {
            val context = getApplication<Application>().applicationContext
            // The three release channels are independent. Start their I/O
            // together so a slow endpoint cannot delay the other checks.
            val visaCheck = async { checkForVisaDatabaseUpdate(context) }
            val guideCheck = async { checkForEntryGuideDatabaseUpdate(context) }
            val requirementCheck = async {
                checkForEntryRequirementDatabaseUpdate(context)
            }

            val result = visaCheck.await()

            when {
                result.database != null -> {
                    applyDatabase(result.database)
                }

                result.checkedAtMillis != null -> {
                    applySuccessfulCheckTimestamp(result.checkedAtMillis)
                }
            }

            val guideResult = guideCheck.await()
            when {
                guideResult.database != null -> {
                    applyEntryGuideDatabase(guideResult.database)
                }

                guideResult.checkedAtMillis != null -> {
                    applyEntryGuideSuccessfulCheckTimestamp(
                        guideResult.checkedAtMillis
                    )
                }
            }

            val entryRequirementResult = requirementCheck.await()
            when {
                entryRequirementResult.database != null -> {
                    applyEntryRequirementDatabase(entryRequirementResult.database)
                }

                entryRequirementResult.checkedAtMillis != null -> {
                    applyEntryRequirementSuccessfulCheckTimestamp(
                        entryRequirementResult.checkedAtMillis
                    )
                }
            }
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

internal fun emptyVisaDatabase(): VisaDatabase =
    VisaDatabase(
        source = "Passport Index Data",
        sourceUrl = "",
        updated = "",
        origin = VisaDatabaseOrigin.BUNDLED,
        version = 0,
        destinationCount = 248,
        lastSuccessfulCheckAt = 0L,
        requirementsByPassport = emptyMap()
    )

internal fun emptyEntryGuideDatabase(): EntryGuideDatabase =
    EntryGuideDatabase(
        version = 0,
        updated = "",
        origin = VisaDatabaseOrigin.BUNDLED,
        lastSuccessfulCheckAt = 0L,
        guides = emptyMap()
    )

internal fun emptyEntryRequirementDatabase(): EntryRequirementDatabase =
    EntryRequirementDatabase(
        version = 0,
        updated = "",
        origin = VisaDatabaseOrigin.BUNDLED,
        lastSuccessfulCheckAt = 0L,
        requirements = emptyMap()
    )
