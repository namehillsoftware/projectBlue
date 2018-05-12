package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress.specs.GivenAPausedFileThatIsThrowingIllegalStateExceptions;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress.MediaPlayerFileProgressReader;

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
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(false);
		when(mockMediaPlayer.getCurrentPosition()).thenThrow(new IllegalStateException());
		when(mockMediaPlayer.getDuration()).thenThrow(new IllegalStateException());

		final MediaPlayerFileProgressReader mediaPlayerFileProgressReader = new MediaPlayerFileProgressReader(mockMediaPlayer);
		progress = mediaPlayerFileProgressReader.getProgress();
	}

	@Test
	public void thenTheFileProgressIsCorrect() {
		assertThat(progress).isEqualTo(Duration.ZERO);
	}
}
