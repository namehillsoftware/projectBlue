package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import android.content.Context;
import android.os.Handler;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider;

public class PreparedPlaybackQueueFeederBuilder implements BuildPreparedPlaybackQueueFeeder {

	private final Context context;
	private final Handler handler;
	private final IConnectionProvider connectionProvider;
	private final BestMatchUriProvider bestMatchUriProvider;
	private final Cache cache;

	public PreparedPlaybackQueueFeederBuilder(
		Context context,
		Handler handler,
		IConnectionProvider connectionProvider,
		BestMatchUriProvider bestMatchUriProvider,
		Cache cache) {

		this.context = context;
		this.handler = handler;
		this.connectionProvider = connectionProvider;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.cache = cache;
	}

	@Override
	public IPlayableFilePreparationSourceProvider build(Library library) {
		return new ExoPlayerPlayableFilePreparationSourceProvider(
			context,
			handler,
			connectionProvider,
			bestMatchUriProvider,
			library,
			cache);
	}
}
