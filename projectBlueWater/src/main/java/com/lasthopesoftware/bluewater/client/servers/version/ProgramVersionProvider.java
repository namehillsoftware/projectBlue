package com.lasthopesoftware.bluewater.client.servers.version;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.namehillsoftware.handoff.promises.Promise;

public class ProgramVersionProvider implements IProgramVersionProvider {

	private final IConnectionProvider connectionProvider;

	private SemanticVersion serverVersion;

	public ProgramVersionProvider(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Promise<SemanticVersion> promiseServerVersion() {
		if (serverVersion != null) return new Promise<>(serverVersion);

		return connectionProvider.promiseResponse("Alive").then(response -> {
			final StandardRequest standardRequest = StandardRequest.fromInputStream(response.body().byteStream());
			if (standardRequest == null) {
				return null;
			}

			final String semVerString = standardRequest.items.get("ProgramVersion");
			if (semVerString == null) {
				return null;
			}

			final String[] semVerParts = semVerString.split("\\.");

			int major = 0, minor = 0, patch = 0;

			if (semVerParts.length > 0)
				major = Integer.parseInt(semVerParts[0]);

			if (semVerParts.length > 1)
				minor = Integer.parseInt(semVerParts[1]);

			if (semVerParts.length > 2)
				patch = Integer.parseInt(semVerParts[2]);

			return serverVersion = new SemanticVersion(major, minor, patch);
		});
	}
}
