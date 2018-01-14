package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.LoadingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;

final class ExoPlayerPreparerTask implements PromisedResponse<Uri, PreparedPlayableFile> {

	private final DataSourceFactoryProvider dataSourceFactoryProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory rendersFactory;
	private final ExtractorsFactory extractorsFactory;
	private final Handler handler;
	private final long prepareAt;
	private final ServiceFile serviceFile;

	ExoPlayerPreparerTask(DataSourceFactoryProvider dataSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory rendersFactory, ExtractorsFactory extractorsFactory, Handler handler, ServiceFile serviceFile, long prepareAt) {
		this.dataSourceFactoryProvider = dataSourceFactoryProvider;
		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
		this.rendersFactory = rendersFactory;
		this.extractorsFactory = extractorsFactory;
		this.handler = handler;
		this.serviceFile = serviceFile;
		this.prepareAt = prepareAt;
	}

	@Override
	public Promise<PreparedPlayableFile> promiseResponse(Uri uri) throws Throwable {
		return new Promise<>(
			new ExoPlayerPreparationOperator(
				dataSourceFactoryProvider,
				trackSelector,
				loadControl,
				rendersFactory,
				extractorsFactory,
				handler,
				serviceFile,
				uri,
				prepareAt));
	}

	private static final class ExoPlayerPreparationOperator implements MessengerOperator<PreparedPlayableFile> {

		private final DataSourceFactoryProvider dataSourceFactoryProvider;
		private final TrackSelector trackSelector;
		private final LoadControl loadControl;
		private final RenderersFactory renderersFactory;
		private final ExtractorsFactory extractorsFactory;
		private final Handler handler;
		private final Uri uri;
		private final long prepareAt;
		private final ServiceFile serviceFile;

		ExoPlayerPreparationOperator(DataSourceFactoryProvider dataSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory renderersFactory, ExtractorsFactory extractorsFactory, Handler handler, ServiceFile serviceFile, Uri uri, long prepareAt) {
			this.dataSourceFactoryProvider = dataSourceFactoryProvider;
			this.trackSelector = trackSelector;
			this.loadControl = loadControl;
			this.renderersFactory = renderersFactory;
			this.extractorsFactory = extractorsFactory;
			this.handler = handler;
			this.serviceFile = serviceFile;
			this.uri = uri;
			this.prepareAt = prepareAt;
		}

		@Override
		public void send(Messenger<PreparedPlayableFile> messenger) {
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

			final LoadingExoPlayer loadingExoPlayer = new LoadingExoPlayer();

			final ExoPlayerPreparationHandler exoPlayerPreparationHandler =
				new ExoPlayerPreparationHandler(exoPlayer,
					loadingExoPlayer,
					prepareAt,
					messenger,
					cancellationToken);

			exoPlayer.addListener(exoPlayerPreparationHandler);

			if (cancellationToken.isCancelled()) return;

			final ExtractorMediaSource.Factory factory =
				new ExtractorMediaSource.Factory(
					dataSourceFactoryProvider.getFactory(uri, serviceFile));
			factory
				.setMinLoadableRetryCount(ExtractorMediaSource.DEFAULT_MIN_LOADABLE_RETRY_COUNT_LIVE);
			final MediaSource mediaSource = factory.createMediaSource(
				uri,
				handler,
				loadingExoPlayer);

			try {
				exoPlayer.prepare(mediaSource);
			} catch (IllegalStateException e) {
				messenger.sendRejection(e);
			}
		}
	}

	private static final class ExoPlayerPreparationHandler
	implements
		Player.EventListener,
		Runnable {
		private static final Logger logger = LoggerFactory.getLogger(ExoPlayerPlaybackHandler.class);

		private final SimpleExoPlayer exoPlayer;
		private final Messenger<PreparedPlayableFile> messenger;
		private final LoadingExoPlayer loadingExoPlayer;
		private final long prepareAt;
		private final CancellationToken cancellationToken;

		private ExoPlayerPreparationHandler(SimpleExoPlayer exoPlayer, LoadingExoPlayer loadingExoPlayer, long prepareAt, Messenger<PreparedPlayableFile> messenger, CancellationToken cancellationToken) {
			this.exoPlayer = exoPlayer;
			this.loadingExoPlayer = loadingExoPlayer;
			this.prepareAt = prepareAt;
			this.messenger = messenger;
			this.cancellationToken = cancellationToken;
			messenger.cancellationRequested(this);
		}

		@Override
		public void run() {
			cancellationToken.run();

			exoPlayer.release();

			messenger.sendRejection(new CancellationException());
		}

		@Override
		public void onTimelineChanged(Timeline timeline, Object manifest) {
		}

		@Override
		public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

		}

		@Override
		public void onLoadingChanged(boolean isLoading) {
		}

		@Override
		public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
			if (cancellationToken.isCancelled()) return;

			if (playbackState != Player.STATE_READY) return;

			if (exoPlayer.getCurrentPosition() < prepareAt) {
				exoPlayer.seekTo(prepareAt);
				return;
			}

			exoPlayer.removeListener(this);

			messenger.sendResolution(new PreparedPlayableFile(new ExoPlayerPlaybackHandler(exoPlayer), loadingExoPlayer));
		}

		@Override
		public void onRepeatModeChanged(int repeatMode) {

		}

		@Override
		public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

		}

		@Override
		public void onPlayerError(ExoPlaybackException error) {
			logger.error("An error occurred while preparing the exo player! Retrying initialization.", error);

			exoPlayer.stop();
			exoPlayer.release();
			messenger.sendRejection(error);
		}

		@Override
		public void onPositionDiscontinuity(int reason) {

		}

		@Override
		public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
		}

		@Override
		public void onSeekProcessed() {

		}
	}
}
