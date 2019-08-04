package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.os.Handler;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.namehillsoftware.handoff.promises.Promise;

public final class ExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private final SpawnMediaSources mediaSourceProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory renderersFactory;
	private final Handler handler;
	private final IFileUriProvider uriProvider;

	public ExoPlayerPlaybackPreparer(
		SpawnMediaSources mediaSourceProvider,
		TrackSelector trackSelector,
		LoadControl loadControl,
		RenderersFactory renderersFactory,
		Handler handler,
		IFileUriProvider uriProvider) {

		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
		this.renderersFactory = renderersFactory;
		this.handler = handler;
		this.uriProvider = uriProvider;
		this.mediaSourceProvider = mediaSourceProvider;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return uriProvider.promiseFileUri(serviceFile)
			.eventually(uri -> new PreparedExoPlayerPromise(
				mediaSourceProvider,
				trackSelector,
				loadControl,
				renderersFactory,
				handler,
				uri,
				preparedAt));
	}
}
