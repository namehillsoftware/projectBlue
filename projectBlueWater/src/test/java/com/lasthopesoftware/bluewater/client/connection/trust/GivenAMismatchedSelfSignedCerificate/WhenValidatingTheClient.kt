package com.lasthopesoftware.bluewater.client.connection.trust.GivenAMismatchedSelfSignedCerificate;

import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WhenValidatingTheClient {

	private static final String certificate =
		"-----BEGIN CERTIFICATE-----\n" +
		"MIIDIjCCAgqgAwIBAgIMWxPfarA/XWVOtWFBMA0GCSqGSIb3DQEBCwUAMBcxFTAT\n" +
		"BgNVBAMTDGpyaXZlci5sb2NhbDAeFw0xODA2MDMxMjMwMzRaFw0yMDA2MDIxMjMw\n" +
		"MzRaMBcxFTATBgNVBAMTDGpyaXZlci5sb2NhbDCCASIwDQYJKoZIhvcNAQEBBQAD\n" +
		"ggEPADCCAQoCggEBAMfIHk7iN3g5d+21YnItl5IUdW2jaK/iiwHd1ZnSwhF2pAKs\n" +
		"oMdfE9wJVFTaMToyHIVlt87HV9I/4x+qqeIZL5cQe7/fk4WBESUpXxHVBJiOM1mB\n" +
		"2XQj3wN1n6qJTPvX2Qpx/Pwjmhn3++aoIg/cqbBz0N2gYNowdwopzYyCNEEMXD93\n" +
		"0DRhq+jlJgVLLbuYBbhpba7SZ55TH0vt8CLMOJDdPIBeEtVp3/uii9hJfE8Q+FDk\n" +
		"2TcwyahKXpzeZEK+vlcs59RjEeCHS+5GkU9+Dq1xp/gQYAzzPaK20R/uzTeEEWNn\n" +
		"hshLsFejNKN9hfZG+d9W80GNZtlEWXePrth6QjECAwEAAaNuMGwwDAYDVR0TAQH/\n" +
		"BAIwADAXBgNVHREEEDAOggxqcml2ZXIubG9jYWwwEwYDVR0lBAwwCgYIKwYBBQUH\n" +
		"AwEwDwYDVR0PAQH/BAUDAwegADAdBgNVHQ4EFgQURfRoVRvz2lIzua1DKAWmrp8t\n" +
		"0hQwDQYJKoZIhvcNAQELBQADggEBAMPGDDISzQ2Hn5WztzXKgWb8ZAOCuQHBUm1K\n" +
		"vJBeg53iq8jTKNEljBugjfGNKmulgtB09VSbfO8LcTf7MM6eecjRzyphw/0qIEaM\n" +
		"7ldU/J8Xehs6HXPbRgGkbYodJzF/TyTU+ZwYVy5orCS+WKcecdqA2G2US7jAr+Wo\n" +
		"aKjrSyZ1OT/6ssBlJfGERMZsxHBSI2E1g5SspxKacIPhsn3iUjvrc+iAvySvkeaA\n" +
		"GDJ4WOGWGKgUazn/G0Ay6YLo2iKATTJW6EI/GgdTiiTKtK7lM5mJJMyJoZHboBPO\n" +
		"IWOVi9WQuTAp+i69AgrXqs7SjRUOTMUbNpv0OCCJGL4s/Y9R3Ms=\n" +
		"-----END CERTIFICATE-----\n";

	private static X509Certificate[] certChain;

	private static final X509TrustManager fallbackTrustManager = mock(X509TrustManager.class);

	private static CertificateException certificateException;

	@BeforeClass
	public static void before() throws CertificateException, IOException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (final InputStream caInput = IOUtils.toInputStream(certificate, "UTF-8")) {
			final X509Certificate cert = (X509Certificate) cf.generateCertificate(caInput);
			certChain = new X509Certificate[] { cert };
		}

		final byte[] certBytes = Hex.decode("1b6fae967b4a43192d3b65bba33cf7fc510df456");
		final SelfSignedTrustManager selfSignedTrustManager = new SelfSignedTrustManager(certBytes, fallbackTrustManager);

		try {
			selfSignedTrustManager.checkServerTrusted(certChain, null);
		} catch (CertificateException e) {
			certificateException = e;
		}
	}

	@Test
	public void thenNoExceptionsAreThrown() {
		assertThat(certificateException).isNull();
	}

	@Test
	public void thenTheFallbackTrustManagerIsCalled() throws CertificateException {
		verify(fallbackTrustManager, atLeastOnce()).checkServerTrusted(certChain, null);
	}
}
