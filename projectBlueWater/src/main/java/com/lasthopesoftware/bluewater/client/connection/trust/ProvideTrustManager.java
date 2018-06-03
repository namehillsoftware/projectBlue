package com.lasthopesoftware.bluewater.client.connection.trust;

import javax.net.ssl.TrustManager;

public interface ProvideTrustManager {
	TrustManager getTrustManager();
}
