package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.factory;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.IFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion;
import com.lasthopesoftware.messenger.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.ILazy;

public class PlaystatsUpdateFactory implements IPlaystatsUpdate {

	private final IConnectionProvider connectionProvider;
	private final IFilePropertiesProvider filePropertiesProvider;
	private final ILazy<Promise<SemanticVersion>> lazyProgramVersion;

	public PlaystatsUpdateFactory(IConnectionProvider connectionProvider, IFilePropertiesProvider filePropertiesProvider) {
		this.connectionProvider = connectionProvider;
		this.filePropertiesProvider = filePropertiesProvider;

		lazyProgramVersion = new AbstractSynchronousLazy<Promise<SemanticVersion>>() {
			@Override
			protected Promise<SemanticVersion> initialize() throws Exception {
				return connectionProvider.promiseConnectionProgramVersion();
			}
		};
	}

	@Override
	public Promise<?> promisePlaystatsUpdate(ServiceFile serviceFile) {
		return null;
	}
}
