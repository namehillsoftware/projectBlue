package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.specs.GivenAPausedFile;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static Duration progress;

	@BeforeClass
	public static void before() {
		final ExoPlayer mockMediaPlayer = mock(ExoPlayer.class);
		when(mockMediaPlayer.getPlayWhenReady()).thenReturn(false);

		final ExoPlayerFileProgressReader exoPlayerFileProgressReader = new ExoPlayerFileProgressReader(mockMediaPlayer);
		progress = exoPlayerFileProgressReader.getFileProgress();
	}

	@Test
	public void thenTheFileProgressIsCorrect() {
		assertThat(progress).isEqualTo(Duration.ZERO);
	}
}
