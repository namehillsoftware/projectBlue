package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.factory;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.IFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater;
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

public class PlaystatsUpdateSelector {

	private final CreateAndHold<Promise<IPlaystatsUpdate>> lazyPlaystatsUpdate;

	public PlaystatsUpdateSelector(IConnectionProvider connectionProvider, IFilePropertiesProvider filePropertiesProvider, FilePropertiesStorage filePropertiesStorage, IProgramVersionProvider programVersionProvider) {
		lazyPlaystatsUpdate = new AbstractSynchronousLazy<Promise<IPlaystatsUpdate>>() {
			@Override
			protected Promise<IPlaystatsUpdate> create() throws Exception {
				return programVersionProvider.promiseServerVersion()
					.then(programVersion -> programVersion != null && programVersion.major >= 22
						? new PlayedFilePlayStatsUpdater(connectionProvider)
						: new FilePropertiesPlayStatsUpdater(filePropertiesProvider, filePropertiesStorage));
			}
		};
	}

	public Promise<IPlaystatsUpdate> promisePlaystatsUpdater() {
		return lazyPlaystatsUpdate.getObject();
	}
}
