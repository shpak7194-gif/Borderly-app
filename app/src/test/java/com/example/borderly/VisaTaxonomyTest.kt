package com.example.borderly

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisaTaxonomyTest {
    @Test
    fun everyCanonicalStorageStatusHasOneUiType() {
        val expected = mapOf(
            "freedom" to VisaType.FREEDOM,
            "visa free" to VisaType.VISA_FREE,
            "eta" to VisaType.ETA,
            "visa on arrival" to VisaType.VISA_ON_ARRIVAL,
            "e-visa" to VisaType.E_VISA,
            "visa required" to VisaType.VISA_REQUIRED,
            "entry restricted" to VisaType.ENTRY_RESTRICTED,
            "special permit" to VisaType.SPECIAL_PERMIT,
            "mixed requirements" to VisaType.MIXED_REQUIREMENTS,
            "no data" to VisaType.NO_DATA
        )

        expected.forEach { (status, type) ->
            assertEquals(type, visaTypeFromStorageStatus(status))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun unknownStorageStatusIsRejected() {
        visaTypeFromStorageStatus("arrival card")
    }

    @Test
    fun visaFreeFilterDoesNotIncludeFreedomOrEta() {
        assertTrue(MapVisaQuickFilter.VISA_FREE.matches(VisaType.VISA_FREE))
        assertFalse(MapVisaQuickFilter.VISA_FREE.matches(VisaType.FREEDOM))
        assertFalse(MapVisaQuickFilter.VISA_FREE.matches(VisaType.ETA))
    }

    @Test
    fun rankingScoresOnlyVisaFreeAndFreedom() {
        assertEquals(setOf(VisaType.FREEDOM, VisaType.VISA_FREE), ScoredVisaTypes)
    }

    @Test
    fun etaUsesUnambiguousUserFacingLabel() {
        assertEquals("eTA/ESTA", VisaType.ETA.title)
        assertEquals("eTA/ESTA", VisaStatusFilter.ETA.title)
    }
}
