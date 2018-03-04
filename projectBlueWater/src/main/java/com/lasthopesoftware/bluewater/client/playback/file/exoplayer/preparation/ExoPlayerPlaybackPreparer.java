package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.os.Handler;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.AudioRenderingEventListener;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;

import java.util.concurrent.CancellationException;

final class ExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory renderersFactory;
	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;

	ExoPlayerPlaybackPreparer(ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory renderersFactory, Handler handler, BestMatchUriProvider bestMatchUriProvider) {
		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
		this.renderersFactory = renderersFactory;
		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.extractorMediaSourceFactoryProvider = extractorMediaSourceFactoryProvider;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return bestMatchUriProvider.promiseFileUri(serviceFile)
			.eventually(uri ->
				new Promise<>(messenger -> {
					final CancellationToken cancellationToken = new CancellationToken();
					messenger.cancellationRequested(cancellationToken);

					if (cancellationToken.isCancelled()) {
						messenger.sendRejection(new CancellationException());
						return;
					}

					final MediaCodecAudioRenderer[] renderers =
						(MediaCodecAudioRenderer[])Stream.of(renderersFactory.createRenderers(
								handler,
								null,
								new AudioRenderingEventListener(),
								null,
								null))
							.filter(r -> r instanceof MediaCodecAudioRenderer)
							.toArray();

					final ExoPlayer exoPlayer = ExoPlayerFactory.newInstance(
						renderers,
						trackSelector,
						loadControl);

					if (cancellationToken.isCancelled()) {
						exoPlayer.release();
						messenger.sendRejection(new CancellationException());
						return;
					}

					final BufferingExoPlayer bufferingExoPlayer = new BufferingExoPlayer();

					final ExoPlayerPreparationHandler exoPlayerPreparationHandler =
						new ExoPlayerPreparationHandler(
							exoPlayer,
							renderers,
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
