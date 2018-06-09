/*
 * Copyright 2010-2013 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lasthopesoftware.bluewater.client.connection.trust;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.X509TrustManager;

public class SelfSignedTrustManager implements X509TrustManager {

	private static final X509Certificate[] acceptedIssuers = new X509Certificate[]{};

	private final byte[] certificateFingerprint;
	private final X509TrustManager fallbackTrustManager;

	public SelfSignedTrustManager(byte[] certificateFingerprint, X509TrustManager fallbackTrustManager) {
		super();
		this.certificateFingerprint = certificateFingerprint;
		this.fallbackTrustManager = fallbackTrustManager;
	}

	// Thank you: http://stackoverflow.com/questions/1270703/how-to-retrieve-compute-an-x509-certificates-thumbprint-in-java
	private static byte[] getThumbPrint(X509Certificate cert)
		throws NoSuchAlgorithmException, CertificateEncodingException {
		final MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(cert.getEncoded());
		return md.digest();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		fallbackTrustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (this.certificateFingerprint == null) {
			throw new CertificateException("Requires a non-null certificateFingerprint key in SHA-1 format to match.");
		}

		// We have a certKey defined. We should now examine the one we got from the server.
		// They match? All is good. They don't, throw an exception.
		try {
			// Assume self-signed root is okay
			if (Arrays.equals(this.certificateFingerprint, getThumbPrint(chain[0]))) return;
		} catch (NoSuchAlgorithmException e) {
			throw new CertificateException("Unable to check self-signed certificate, unknown algorithm. ", e);
		}

		fallbackTrustManager.checkServerTrusted(chain, authType);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return acceptedIssuers;
	}

}
