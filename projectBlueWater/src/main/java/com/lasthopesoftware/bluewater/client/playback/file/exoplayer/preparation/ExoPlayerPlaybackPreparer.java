package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.os.Handler;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.MediaSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.namehillsoftware.handoff.promises.Promise;

final class ExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private final MediaSourceProvider mediaSourceProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory renderersFactory;
	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;

	ExoPlayerPlaybackPreparer(
		MediaSourceProvider mediaSourceProvider,
		TrackSelector trackSelector,
		LoadControl loadControl,
		RenderersFactory renderersFactory,
		Handler handler,
		BestMatchUriProvider bestMatchUriProvider) {

		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
		this.renderersFactory = renderersFactory;
		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.mediaSourceProvider = mediaSourceProvider;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return bestMatchUriProvider.promiseFileUri(serviceFile)
			.eventually(uri -> new PromisePreparedExoPlayer(
				mediaSourceProvider,
				trackSelector,
				loadControl,
				renderersFactory,
				handler,
				uri,
				preparedAt));
	}
}
