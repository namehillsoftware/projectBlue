package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAMediaPlayerEngineSelection;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngineBuilder;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.mediaplayer.MediaPlayerPlaybackPreparerProvider;

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
		when(lookupSelectedPlaybackEngineType.getSelectedPlaybackEngineType())
			.thenReturn(PlaybackEngineType.MediaPlayer);

		final PlaybackEngineBuilder playbackEngineBuilder =
			new PlaybackEngineBuilder(
				mock(Context.class),
				mock(IFileUriProvider.class),
				new Library(),
				lookupSelectedPlaybackEngineType);

		engine = playbackEngineBuilder.build();
	}

	@Test
	public void thenAMediaPlayerEngineIsBuilt() {
		assertThat(engine).isInstanceOf(MediaPlayerPlaybackPreparerProvider.class);
	}
}
