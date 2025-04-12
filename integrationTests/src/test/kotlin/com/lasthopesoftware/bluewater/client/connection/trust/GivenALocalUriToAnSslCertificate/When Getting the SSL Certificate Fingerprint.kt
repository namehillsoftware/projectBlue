package com.lasthopesoftware.bluewater.client.connection.trust.GivenALocalUriToAnSslCertificate

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.connection.trust.UserSslCertificateProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.net.URI

@RunWith(AndroidJUnit4::class)
class `When Getting the SSL Certificate Fingerprint` {

	companion object {

		private const val certificate = """
-----BEGIN CERTIFICATE-----
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

		private val service by lazy {
            UserSslCertificateProvider(
                mockk {
                    every {
                        promiseSelectedDocumentUri(
                            "application/x-x509-ca-cert",
                            "application/x-x509-user-cert",
                            "application/x-pem-file",
                        )
                    } returns URI("wl2Ta0").toPromise()
                },
                mockk {
                    every { openInputStream(Uri.parse("wl2Ta0")) } returns ByteArrayInputStream(
                        certificate.trim().toByteArray(Charsets.UTF_8)
                    )
                }
            )
		}

		private var fingerprint: ByteArray? = null

		@BeforeClass
		@JvmStatic
		fun act() {
			fingerprint = service.promiseUserSslCertificateFingerprint().toExpiringFuture().get()
		}
	}

	@Test
	fun `then the fingerprint is correct`() {
		Assertions.assertThat(fingerprint).asHexString().isEqualToIgnoringCase("1b6fae967b4a13192d3b65bba33cf7fc510df456")
	}
}
