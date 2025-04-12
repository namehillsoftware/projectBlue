package com.lasthopesoftware.bluewater.client.connection.trust.GivenAMismatchedSelfSignedCerificate

import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager
import io.mockk.mockk
import io.mockk.verify
import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.IOUtils
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

private const val certificate = """-----BEGIN CERTIFICATE-----
MIIDIjCCAgqgAwIBAgIMWxPfarA/XWVOtWFBMA0GCSqGSIb3DQEBCwUAMBcxFTAT
BgNVBAMTDGpyaXZlci5sb2NhbDAeFw0xODA2MDMxMjMwMzRaFw0yMDA2MDIxMjMw
MzRaMBcxFTATBgNVBAMTDGpyaXZlci5sb2NhbDCCASIwDQYJKoZIhvcNAQEBBQAD
ggEPADCCAQoCggEBAMfIHk7iN3g5d+21YnItl5IUdW2jaK/iiwHd1ZnSwhF2pAKs
oMdfE9wJVFTaMToyHIVlt87HV9I/4x+qqeIZL5cQe7/fk4WBESUpXxHVBJiOM1mB
2XQj3wN1n6qJTPvX2Qpx/Pwjmhn3++aoIg/cqbBz0N2gYNowdwopzYyCNEEMXD93
0DRhq+jlJgVLLbuYBbhpba7SZ55TH0vt8CLMOJDdPIBeEtVp3/uii9hJfE8Q+FDk
2TcwyahKXpzeZEK+vlcs59RjEeCHS+5GkU9+Dq1xp/gQYAzzPaK20R/uzTeEEWNn
hshLsFejNKN9hfZG+d9W80GNZtlEWXePrth6QjECAwEAAaNuMGwwDAYDVR0TAQH/
BAIwADAXBgNVHREEEDAOggxqcml2ZXIubG9jYWwwEwYDVR0lBAwwCgYIKwYBBQUH
AwEwDwYDVR0PAQH/BAUDAwegADAdBgNVHQ4EFgQURfRoVRvz2lIzua1DKAWmrp8t
0hQwDQYJKoZIhvcNAQELBQADggEBAMPGDDISzQ2Hn5WztzXKgWb8ZAOCuQHBUm1K
vJBeg53iq8jTKNEljBugjfGNKmulgtB09VSbfO8LcTf7MM6eecjRzyphw/0qIEaM
7ldU/J8Xehs6HXPbRgGkbYodJzF/TyTU+ZwYVy5orCS+WKcecdqA2G2US7jAr+Wo
aKjrSyZ1OT/6ssBlJfGERMZsxHBSI2E1g5SspxKacIPhsn3iUjvrc+iAvySvkeaA
GDJ4WOGWGKgUazn/G0Ay6YLo2iKATTJW6EI/GgdTiiTKtK7lM5mJJMyJoZHboBPO
IWOVi9WQuTAp+i69AgrXqs7SjRUOTMUbNpv0OCCJGL4s/Y9R3Ms=
-----END CERTIFICATE-----
"""

class WhenValidatingTheClient {
	private val certChain by lazy {
		val cf = CertificateFactory.getInstance("X.509")
		IOUtils.toInputStream(certificate, "UTF-8").use { caInput ->
			val cert = cf.generateCertificate(caInput) as X509Certificate
			arrayOf(cert)
		}
	}
	private val fallbackTrustManager = mockk<X509TrustManager>(relaxed = true)
	private var certificateException: CertificateException? = null

	@BeforeAll
	@Throws(CertificateException::class, IOException::class)
	fun before() {
		val certBytes = Hex.decodeHex("1b6fae967b4a43192d3b65bba33cf7fc510df456")
		val selfSignedTrustManager = SelfSignedTrustManager(certBytes, fallbackTrustManager)
		try {
			selfSignedTrustManager.checkServerTrusted(certChain, null)
		} catch (e: CertificateException) {
			certificateException = e
		}
	}

	@Test
	fun `then no exceptions are thrown`() {
		assertThat(certificateException).isNull()
	}

	@Test
	fun `then the fallback trust manager is called`() {
		verify(atLeast = 1) { fallbackTrustManager.checkServerTrusted(certChain, null) }
	}
}
