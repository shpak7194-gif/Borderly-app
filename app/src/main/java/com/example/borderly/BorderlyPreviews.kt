package com.example.borderly

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.borderly.ui.theme.BorderlyTheme

/**
 * Android Studio Preview only.
 *
 * Runtime startup never calls these synchronous asset loaders; production data
 * loading is owned by BorderlyViewModel and runs on Dispatchers.IO.
 */
@Composable
private fun BorderlyPreviewContent() {
    val context = LocalContext.current
    val previewMap = remember {
        runCatching { loadNativeWorldMap(context) }.getOrNull()
    }
    val previewDatabase = remember {
        runCatching { loadVisaDatabase(context) }.getOrElse {
            emptyVisaDatabase()
        }
    }
    val previewEntryGuides = remember {
        runCatching { loadEntryGuideDatabase(context) }.getOrElse {
            emptyEntryGuideDatabase()
        }
    }
    val previewEntryRequirements = remember {
        runCatching { loadEntryRequirementDatabase(context) }.getOrElse {
            emptyEntryRequirementDatabase()
        }
    }

    BorderlyHomeContent(
        nativeMap = previewMap,
        visaDatabase = previewDatabase,
        entryGuideDatabase = previewEntryGuides,
        entryRequirementDatabase = previewEntryRequirements,
        themeMode = AppThemeMode.LIGHT,
        onThemeModeChange = {}
    )
}

@Preview(name = "Обычный телефон", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
internal fun BorderlyPreview() {
    BorderlyTheme {
        BorderlyPreviewContent()
    }
}

@Preview(name = "Узкий телефон", showBackground = true, widthDp = 320, heightDp = 720)
@Composable
internal fun BorderlyCompactPreview() {
    BorderlyTheme {
        BorderlyPreviewContent()
    }
}

@Preview(name = "Широкий телефон", showBackground = true, widthDp = 432, heightDp = 900)
@Composable
internal fun BorderlyWidePreview() {
    BorderlyTheme {
        BorderlyPreviewContent()
    }
}
