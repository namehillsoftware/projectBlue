package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.factory;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.ProvideFilePropertiesForSession;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider;
import com.namehillsoftware.handoff.promises.Promise;

public class PlaystatsUpdateSelector {

	private final IConnectionProvider connectionProvider;
	private final ProvideFilePropertiesForSession filePropertiesProvider;
	private final FilePropertiesStorage filePropertiesStorage;
	private final IProgramVersionProvider programVersionProvider;

	private volatile Promise<IPlaystatsUpdate> promisedPlaystatsUpdater = Promise.empty();

	public PlaystatsUpdateSelector(IConnectionProvider connectionProvider, ProvideFilePropertiesForSession filePropertiesProvider, FilePropertiesStorage filePropertiesStorage, IProgramVersionProvider programVersionProvider) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesProvider = filePropertiesProvider;
		this.filePropertiesStorage = filePropertiesStorage;
		this.programVersionProvider = programVersionProvider;
	}

	public synchronized Promise<IPlaystatsUpdate> promisePlaystatsUpdater() {
		return promisedPlaystatsUpdater = promisedPlaystatsUpdater.eventually(
				u -> u != null ? new Promise<>(u) : promiseNewPlaystatsUpdater(),
				e -> promiseNewPlaystatsUpdater());
	}

	private Promise<IPlaystatsUpdate> promiseNewPlaystatsUpdater() {
		return programVersionProvider.promiseServerVersion()
			.then(programVersion -> programVersion != null && programVersion.major >= 22
					? new PlayedFilePlayStatsUpdater(connectionProvider)
					: new FilePropertiesPlayStatsUpdater(filePropertiesProvider, filePropertiesStorage));
	}
}
