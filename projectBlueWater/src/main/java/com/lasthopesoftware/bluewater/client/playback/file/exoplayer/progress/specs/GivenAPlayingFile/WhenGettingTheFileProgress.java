package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.specs.GivenAPlayingFile;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static FileProgress progress;

	@BeforeClass
	public static void before() {
		final ExoPlayer mockMediaPlayer = mock(ExoPlayer.class);
		when(mockMediaPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(75L);
		when(mockMediaPlayer.getDuration()).thenReturn(101L);

		final ExoPlayerFileProgressReader exoPlayerFileProgressReader = new ExoPlayerFileProgressReader(mockMediaPlayer);
		progress = exoPlayerFileProgressReader.getFileProgress();
	}

	@Test
	public void thenTheFileProgressIsCorrect() {
		assertThat(progress).isEqualTo(new FileProgress(75, 101));
	}
}
