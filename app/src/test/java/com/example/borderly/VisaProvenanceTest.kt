package com.example.borderly

import org.junit.Assert.assertEquals
import org.junit.Test

class VisaProvenanceTest {
    @Test
    fun storageSourceTypesMapToUiSourceTypes() {
        assertEquals(VisaSourceType.OFFICIAL, VisaSourceType.fromStorage("official"))
        assertEquals(VisaSourceType.CORROBORATED, VisaSourceType.fromStorage("corroborated"))
        assertEquals(VisaSourceType.DATASET, VisaSourceType.fromStorage("dataset"))
        assertEquals(VisaSourceType.DERIVED, VisaSourceType.fromStorage("derived"))
    }

    @Test
    fun unknownSourceTypeIsNotPresentedAsOfficial() {
        assertEquals(VisaSourceType.UNKNOWN, VisaSourceType.fromStorage("government-ish"))
        assertEquals(VisaSourceType.UNKNOWN, VisaSourceType.fromStorage(null))
    }
}
