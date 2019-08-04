package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.content.Context;
import android.os.Handler;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.AudioRenderersFactory;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import org.joda.time.Minutes;


public class ExoPlayerPlayableFilePreparationSourceProvider implements IPlayableFilePreparationSourceProvider {

	private static final CreateAndHold<Integer> maxBufferMs = new Lazy<>(() -> (int) Minutes.minutes(5).toStandardDuration().getMillis());
	private static final CreateAndHold<TrackSelector> trackSelector = new Lazy<>(ExoPlayerPlayableFilePreparationSourceProvider::getNewTrackSelector);
	private static final CreateAndHold<LoadControl> loadControl = new Lazy<>(ExoPlayerPlayableFilePreparationSourceProvider::getNewLoadControl);

	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;
	private final SpawnMediaSources mediaSourceProvider;
	private final RenderersFactory renderersFactory;

	public ExoPlayerPlayableFilePreparationSourceProvider(Context context, Handler handler, SpawnMediaSources mediaSourceProvider, BestMatchUriProvider bestMatchUriProvider) {
		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;

		this.mediaSourceProvider = mediaSourceProvider;

		renderersFactory = new AudioRenderersFactory(context);
	}

	@Override
	public int getMaxQueueSize() {
		return 1;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new ExoPlayerPlaybackPreparer(
			mediaSourceProvider,
			trackSelector.getObject(),
			loadControl.getObject(),
			renderersFactory,
			handler,
			bestMatchUriProvider);
	}

	private static TrackSelector getNewTrackSelector() {
		return new DefaultTrackSelector();
	}

	private static LoadControl getNewLoadControl() {
		final DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();
		builder.setBufferDurationsMs(
			DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
			maxBufferMs.getObject(),
			DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
			DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);

		return builder.createDefaultLoadControl();
	}
}
