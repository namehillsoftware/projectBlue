package com.lasthopesoftware.bluewater.client.servers.version;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class ServerVersionProvider implements IServerVersionProvider {

	private final Object syncObject = new Object();
	private final IConnectionProvider connectionProvider;

	private ProgramVersion programVersion;
	private volatile int serverVersionThreads;

	public ServerVersionProvider(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Promise<ProgramVersion> promiseServerVersion() {
		return new Promise<>((messenger) -> {
			if (programVersion != null) {
				messenger.sendResolution(programVersion);
				return;
			}

			new Thread(() -> {
				try {
					synchronized (syncObject) {
						if (programVersion != null) {
							messenger.sendResolution(programVersion);
							return;
						}

						final HttpURLConnection connection;
						try {
							connection = connectionProvider.getConnection("Alive");
						} catch (IOException e) {
							messenger.sendRejection(e);
							return;
						}

						try {
							try (final InputStream is = connection.getInputStream()) {
								final StandardRequest standardRequest = StandardRequest.fromInputStream(is);
								if (standardRequest == null) {
									messenger.sendResolution(null);
									return;
								}

								final String semVerString = standardRequest.items.get("ProgramVersion");
								if (semVerString == null) {
									messenger.sendResolution(null);
									return;
								}

								final String[] semVerParts = semVerString.split("\\.");

								int major = 0, minor = 0, patch = 0;

								if (semVerParts.length > 0)
									major = Integer.parseInt(semVerParts[0]);

								if (semVerParts.length > 1)
									minor = Integer.parseInt(semVerParts[1]);

								if (semVerParts.length > 2)
									patch = Integer.parseInt(semVerParts[2]);

								programVersion = new ProgramVersion(major, minor, patch);
								messenger.sendResolution(programVersion);
							} catch (IOException e) {
								messenger.sendRejection(e);
							}
						} finally {
							connection.disconnect();
						}
					}
				} finally {
					--serverVersionThreads;
				}
			}, "server-version-thread-" + serverVersionThreads++).run();
		});
	}
}
