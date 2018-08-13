package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.os.Handler;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.AudioRenderingEventListener;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.MetadataOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.TextOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.QueueMediaSources;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.compilation.DebugFlag;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

public class SingleExoPlayerPlaybackPreparer implements PlayableFilePreparationSource {

	private static final CreateAndHold<TextOutputLogger> lazyTextOutputLogger = new Lazy<>(TextOutputLogger::new);
	private static final CreateAndHold<MetadataOutputLogger> lazyMetadataOutputLogger = new Lazy<>(MetadataOutputLogger::new);

	private final ExoPlayer exoPlayer;
	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final QueueMediaSources mediaSourcesQueue;
	private final RenderersFactory renderersFactory;
	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;

	public SingleExoPlayerPlaybackPreparer(ExoPlayer exoPlayer, ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider, QueueMediaSources mediaSourcesQueue, RenderersFactory renderersFactory, Handler handler, BestMatchUriProvider bestMatchUriProvider) {
		this.exoPlayer = exoPlayer;
		this.extractorMediaSourceFactoryProvider = extractorMediaSourceFactoryProvider;
		this.mediaSourcesQueue = mediaSourcesQueue;
		this.renderersFactory = renderersFactory;
		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		return bestMatchUriProvider.promiseFileUri(serviceFile)
			.eventually(uri -> {
				final Renderer[] renderers =
					renderersFactory.createRenderers(
						handler,
						null,
						DebugFlag.getInstance().isDebugCompilation() ? new AudioRenderingEventListener() : null,
						lazyTextOutputLogger.getObject(),
						lazyMetadataOutputLogger.getObject(),
						null);

				final BufferingExoPlayer bufferingExoPlayer = new BufferingExoPlayer();

				final MediaSource mediaSource =
					extractorMediaSourceFactoryProvider
						.getFactory(uri)
						.createMediaSource(uri);

				final PreparedMediaSourcePromise preparedMediaSourcePromise = new PreparedMediaSourcePromise(
					exoPlayer,
					mediaSource,
					handler,
					Stream.of(renderers)
						.filter(r -> r instanceof MediaCodecAudioRenderer)
						.toArray(MediaCodecAudioRenderer[]::new),
					bufferingExoPlayer,
					preparedAt);

				mediaSourcesQueue.enqueueMediaSource(mediaSource);

				return preparedMediaSourcePromise;
			});
	}
}
