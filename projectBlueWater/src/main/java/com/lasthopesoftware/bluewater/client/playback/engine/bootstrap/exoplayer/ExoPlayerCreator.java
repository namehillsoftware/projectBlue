package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.exoplayer;

import android.os.Handler;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.events.AudioRenderingEventListener;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.events.MetadataOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.events.TextOutputLogger;
import com.lasthopesoftware.compilation.DebugFlag;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import org.joda.time.Minutes;

public class ExoPlayerCreator implements CreateExoPlayers {

	private static final CreateAndHold<TrackSelector> trackSelector = new Lazy<>(DefaultTrackSelector::new);
	private static final CreateAndHold<LoadControl> loadControl = new AbstractSynchronousLazy<LoadControl>() {
		@Override
		protected LoadControl create() {
			final DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();
			builder.setBufferDurationsMs(
					DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
					(int) Minutes.minutes(5).toStandardDuration().getMillis(),
					DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
					DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);

			return builder.createDefaultLoadControl();
		}
	};

	private static final CreateAndHold<TextOutputLogger> lazyTextOutputLogger = new Lazy<>(TextOutputLogger::new);
	private static final CreateAndHold<MetadataOutputLogger> lazyMetadataOutputLogger = new Lazy<>(MetadataOutputLogger::new);

	private final Handler handler;
	private final RenderersFactory renderersFactory;

	public ExoPlayerCreator(Handler handler, RenderersFactory renderersFactory) {
		this.handler = handler;
		this.renderersFactory = renderersFactory;
	}

	@Override
	public ExoPlayer createExoPlayer() {
		final Renderer[] renderers = renderersFactory.createRenderers(
				handler,
				null,
				DebugFlag.getInstance().isDebugCompilation() ? new AudioRenderingEventListener() : null,
				lazyTextOutputLogger.getObject(),
				lazyMetadataOutputLogger.getObject(),
				null);

		return ExoPlayerFactory.newInstance(
				renderers,
				trackSelector.getObject(),
				loadControl.getObject());
	}
}
