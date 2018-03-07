package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.os.Handler;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
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
import com.lasthopesoftware.resources.loopers.LooperThreadCreator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class ExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private static final CreateAndHold<Promise<Handler>> rendererHandler = new AbstractSynchronousLazy<Promise<Handler>>() {
		@Override
		protected Promise<Handler> create() throws Throwable {
			return LooperThreadCreator.promiseNewLooperThread("Audio Rendering thread")
				.then(Handler::new);
		}
	};

	private static final CreateAndHold<Promise<Handler>> extractorHandler = new AbstractSynchronousLazy<Promise<Handler>>() {
		@Override
		protected Promise<Handler> create() throws Throwable {
			return LooperThreadCreator.promiseNewLooperThread("Media Extracting thread")
				.then(Handler::new);
		}
	};

	private static final Executor preparationExecutor = Executors.newSingleThreadExecutor();

	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory renderersFactory;
	private final BestMatchUriProvider bestMatchUriProvider;

	ExoPlayerPlaybackPreparer(ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory renderersFactory, BestMatchUriProvider bestMatchUriProvider) {
		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
		this.renderersFactory = renderersFactory;
		this.bestMatchUriProvider = bestMatchUriProvider;
		this.extractorMediaSourceFactoryProvider = extractorMediaSourceFactoryProvider;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return bestMatchUriProvider.promiseFileUri(serviceFile)
			.eventually(uri -> rendererHandler.getObject().eventually(rh ->
				extractorHandler.getObject().eventually(eh ->
					new QueuedPromise<>(messenger -> {
						final CancellationToken cancellationToken = new CancellationToken();
						messenger.cancellationRequested(cancellationToken);

						if (cancellationToken.isCancelled()) {
							messenger.sendRejection(new CancellationException());
							return;
						}

						final Renderer[] renderers =
							renderersFactory.createRenderers(
								rh,
								null,
								AudioRenderingEventListener.isDebugEnabled() ? new AudioRenderingEventListener() : null,
								new TextOutputLogger(),
								new MetadataOutputLogger());

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
								.createMediaSource(
									uri,
									eh,
									bufferingExoPlayer);

						try {
							exoPlayer.prepare(mediaSource);
						} catch (IllegalStateException e) {
							messenger.sendRejection(e);
						}
			}, preparationExecutor))));
	}
}
