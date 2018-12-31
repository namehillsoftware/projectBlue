package com.lasthopesoftware.bluewater.client.playback.engine.preparation;

import android.content.Context;
import android.os.Handler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;

public class PreparedPlaybackQueueFeederBuilder implements BuildPreparedPlaybackQueueFeeder {

	private final Context context;
	private final Handler handler;
	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final BestMatchUriProvider bestMatchUriProvider;

	public PreparedPlaybackQueueFeederBuilder(
		Context context,
		Handler handler,
		ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider,
		BestMatchUriProvider bestMatchUriProvider) {

		this.context = context;
		this.handler = handler;
		this.extractorMediaSourceFactoryProvider = extractorMediaSourceFactoryProvider;
		this.bestMatchUriProvider = bestMatchUriProvider;
	}

	@Override
	public IPlayableFilePreparationSourceProvider build(Library library) {
		return new ExoPlayerPlayableFilePreparationSourceProvider(
			context,
			handler,
			extractorMediaSourceFactoryProvider,
			bestMatchUriProvider
		);
	}
}
