package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.os.Handler;
import com.annimon.stream.Stream;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.AudioRenderingEventListener;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.MetadataOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.TextOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.compilation.DebugFlag;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.util.concurrent.CancellationException;

final class ExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private static final CreateAndHold<TextOutputLogger> lazyTextOutputLogger = new Lazy<>(TextOutputLogger::new);
	private static final CreateAndHold<MetadataOutputLogger> lazyMetadataOutputLogger = new Lazy<>(MetadataOutputLogger::new);

	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory renderersFactory;
	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;

	ExoPlayerPlaybackPreparer(
		ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider,
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

					final Renderer[] renderers =
						renderersFactory.createRenderers(
							handler,
							null,
							DebugFlag.getInstance().isDebugCompilation() ? new AudioRenderingEventListener() : null,
							lazyTextOutputLogger.getObject(),
							lazyMetadataOutputLogger.getObject(),
							null);

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
							Stream.of(renderers)
								.filter(r -> r instanceof MediaCodecAudioRenderer)
								.toArray(MediaCodecAudioRenderer[]::new),
							bufferingExoPlayer,
							preparedAt,
							messenger,
							cancellationToken);

					exoPlayer.addListener(exoPlayerPreparationHandler);

					if (cancellationToken.isCancelled()) return;

					final MediaSource mediaSource =
						extractorMediaSourceFactoryProvider
							.getFactory(uri)
							.createMediaSource(uri);

					try {
						mediaSource.addEventListener(handler, bufferingExoPlayer);
						exoPlayer.prepare(mediaSource);
					} catch (IllegalStateException e) {
						messenger.sendRejection(e);
					}
		}));
	}
}
