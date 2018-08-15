package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenASingleExoPlayerEngine.AndItIsNotCompiledForDebug;

import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.RenderersFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.QueueMediaSources;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.SingleExoPlayerSourcePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenBuildingTheEngine {

	private static Object engine;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final LookupSelectedPlaybackEngineType lookupSelectedPlaybackEngineType =
			mock(LookupSelectedPlaybackEngineType.class);
		when(lookupSelectedPlaybackEngineType.promiseSelectedPlaybackEngineType())
			.thenReturn(new Promise<>(PlaybackEngineType.SingleExoPlayer));

		final PreparedPlaybackQueueFeederBuilder playbackEngineBuilder =
			new PreparedPlaybackQueueFeederBuilder(
				lookupSelectedPlaybackEngineType,
				mock(Handler.class),
				mock(BestMatchUriProvider.class),
				mock(ExtractorMediaSourceFactoryProvider.class),
				mock(ExoPlayer.class),
				mock(QueueMediaSources.class),
				mock(RenderersFactory.class));

		engine = new FuturePromise<>(playbackEngineBuilder.build(new Library())).get();
	}

	@Test
	public void thenASingleExoPlayerEngineIsBuilt() {
		assertThat(engine).isInstanceOf(SingleExoPlayerSourcePreparationSourceProvider.class);
	}
}
