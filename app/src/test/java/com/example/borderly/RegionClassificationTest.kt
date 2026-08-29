package com.example.borderly

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionClassificationTest {
    @Test
    fun everySupportedIsoCodeBelongsToExactlyOneRegion() {
        val regionSets = listOf(
            EuropeanPassportIds,
            AsianPassportIds,
            AmericanPassportIds,
            AfricanPassportIds,
            OceanianPassportIds
        )
        val assignments = regionSets
            .flatMap { it }
            .groupingBy { it }
            .eachCount()

        assertEquals(IsoCountryCodesByNumeric.keys, assignments.keys)
        assertTrue(assignments.values.all { it == 1 })
    }

    @Test
    fun disputedPreviousAssignmentsFollowUnM49() {
        assertEquals(PassportRegion.AFRICA, passportRegionFor(86))
        assertEquals(PassportRegion.AFRICA, passportRegionFor(260))
        assertEquals(PassportRegion.ASIA, passportRegionFor(196))
        assertEquals(PassportRegion.EUROPE, passportRegionFor(643))
        assertEquals(PassportRegion.AMERICAS, passportRegionFor(304))
    }
}
