package com.example.borderly

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisaDownloadRetryTest {
    @Test
    fun transientNetworkFailuresAreRetryable() {
        assertTrue(isRetryableVisaDownloadError(UnknownHostException()))
        assertTrue(isRetryableVisaDownloadError(ConnectException()))
        assertTrue(isRetryableVisaDownloadError(SocketTimeoutException()))
        assertTrue(isRetryableVisaDownloadError(RemoteHttpException(429)))
        assertTrue(isRetryableVisaDownloadError(RemoteHttpException(503)))
    }

    @Test
    fun permanentHttpAndValidationFailuresAreNotRetried() {
        assertFalse(isRetryableVisaDownloadError(RemoteHttpException(404)))
        assertFalse(isRetryableVisaDownloadError(IllegalArgumentException("SHA-256 mismatch")))
    }
}
