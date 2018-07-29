package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenAnExoPlayerEngineSelection.AndItIsNotCompiledForDebug;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.upstream.cache.Cache;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenBuildingTheEngine {

	private static Object engine;

	@BeforeClass
	public static void before() {
		final LookupSelectedPlaybackEngineType lookupSelectedPlaybackEngineType =
			mock(LookupSelectedPlaybackEngineType.class);
		when(lookupSelectedPlaybackEngineType.promiseSelectedPlaybackEngineType())
			.thenReturn(new Promise<>(PlaybackEngineType.ExoPlayer));

		final PreparedPlaybackQueueFeederBuilder playbackEngineBuilder =
			new PreparedPlaybackQueueFeederBuilder(
				mock(Context.class),
				mock(Handler.class),
				mock(IConnectionProvider.class),
				mock(BestMatchUriProvider.class),
				mock(Cache.class));

		engine = playbackEngineBuilder.build(new Library());
	}

	@Test
	public void thenAnExoPlayerEngineIsBuilt() {
		assertThat(engine).isInstanceOf(ExoPlayerPlayableFilePreparationSourceProvider.class);
	}
}
