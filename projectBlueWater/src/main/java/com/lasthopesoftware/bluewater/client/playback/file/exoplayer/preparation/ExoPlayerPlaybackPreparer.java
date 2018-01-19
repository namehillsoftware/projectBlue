package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.resources.uri.PathAndQuery;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;

import java.util.concurrent.CancellationException;

final class ExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final RemoteFileUriProvider remoteFileUriProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory renderersFactory;
	private final Handler handler;
	private final DiskFileCache diskFileCache;
	private final BestMatchUriProvider bestMatchUriProvider;

	ExoPlayerPlaybackPreparer(ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory renderersFactory, Handler handler, DiskFileCache diskFileCache, BestMatchUriProvider bestMatchUriProvider, RemoteFileUriProvider remoteFileUriProvider) {
		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
		this.renderersFactory = renderersFactory;
		this.handler = handler;
		this.diskFileCache = diskFileCache;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.extractorMediaSourceFactoryProvider = extractorMediaSourceFactoryProvider;
		this.remoteFileUriProvider = remoteFileUriProvider;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return remoteFileUriProvider.getFileUri(serviceFile)
			.eventually(uri -> diskFileCache.promiseCachedFile(PathAndQuery.forUri(uri)))
			.eventually(file -> file != null
				? new Promise<>(Uri.fromFile(file))
				: bestMatchUriProvider.getFileUri(serviceFile))
			.eventually(uri ->
				new Promise<>(messenger -> {
					final CancellationToken cancellationToken = new CancellationToken();
					messenger.cancellationRequested(cancellationToken);

					if (cancellationToken.isCancelled()) {
						messenger.sendRejection(new CancellationException());
						return;
					}

					final SimpleExoPlayer exoPlayer = ExoPlayerFactory.newSimpleInstance(
						renderersFactory,
						trackSelector,
						loadControl);
					if (cancellationToken.isCancelled()) {
						exoPlayer.release();
						messenger.sendRejection(new CancellationException());
						return;
					}

					final BufferingExoPlayer bufferingExoPlayer = new BufferingExoPlayer();

					final ExoPlayerPreparationHandler exoPlayerPreparationHandler =
						new ExoPlayerPreparationHandler(exoPlayer,
							bufferingExoPlayer,
							preparedAt,
							messenger,
							cancellationToken);

					exoPlayer.addListener(exoPlayerPreparationHandler);

					if (cancellationToken.isCancelled()) return;

					final MediaSource mediaSource =
						extractorMediaSourceFactoryProvider
							.getFactory(uri)
							.createMediaSource(
								uri,
								handler,
								bufferingExoPlayer);

					try {
						exoPlayer.prepare(mediaSource);
					} catch (IllegalStateException e) {
						messenger.sendRejection(e);
					}
				}));
	}
}
